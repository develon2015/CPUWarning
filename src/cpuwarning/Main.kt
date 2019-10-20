package cpuwarning

import lib.process.Shell
import java.util.Date
import java.io.File

fun main() {
	// 腾讯企业邮箱认账账号
	val sender = MailSender("cpuwarning@githmb.com", "pawd")
	// 邮件接收者
	val sendto = "dest@example.com"

	val sh = Shell()
	val cmd = """top -bi -n 1"""
	sh.ready()

	var logDir = File("./logs") // 日志目录, 记录触发警报的场景
	if (!logDir.exists()) logDir.mkdirs()
	if (!logDir.isDirectory()) logDir = File(".") // 如果无法创建目录logs, 那么不如把日志写到当前目录下

	if (!sh.isAlive()) return println("启动shell失败")
	sh.run(cmd) // 测试 top 命令
	if (sh.lastCode() != 0) {
		println("您的计算机可能不支持top命令")
		return sh.exit()
	}
	println("Shell进程(${ sh.pid })正在保护您的CPU")

/*
	  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
*/
	
	val partyTime = 2000L // 非警戒状态的休闲时间/ms
	val warningTime = 200L // 警戒状态的取样时间
	var warning = false // 警戒状态?
	var time = 0L // 进入警戒状态的时间点
	var i = 0L // 采样次数
	var cpuUsage = 0.0 // CPU累积
	
	var lastSendMailTime = 0L

	while (true) {
		try {
			if (!sh.isAlive()) {
				println("Shell 进程 (PID: ${ sh.pid }) 失控, 请管理员调查")
				sh.ready()
			}
			val output = sh.run(cmd, 2000, 200) ?: ""
//			println(output)
			val regex = """^.*\s+(\d+\.\d+)\s+(\d+.\d+)\s+.*$""".toRegex()
			// 匹配输出
			var cpucount = 0.0
			var memcount = 0.0
			for (line in output.split('\n')) {
				val mr = regex.matchEntire(line)
				if (mr == null) continue
				val (cpu, mem) = mr.destructured
//				println("$cpu $mem")
				cpucount += cpu.toDouble()
				memcount += mem.toDouble()
			}
			
			// CPU异常, 如果没有戒备则进入警戒
			if (cpucount > 80.0) {
				if (!warning) { // 进入警戒模式, 记录时间点
					println("CPU $cpucount %, 进入警戒状态 -- (${ Date() })\n$output")
					warning = true
					i = 0
					cpuUsage = 0.0
					time = System.currentTimeMillis()
					// 记录触发场景
					sh.run("echo '${ output.replace("'", "''") }' >> ${ logDir.getAbsolutePath() }/cpuwarning.\$(date +%Y-%m-%d).log")
				}
			}
			
			// 警戒状态, 要么在达到底线时发送警报, 要么确认安全后解除警戒
			if (warning) {
				println("采样 $i \tCPU: ${ cpucount }\tMem: ${ memcount }")
				cpuUsage += cpucount
				i ++
				val crt = System.currentTimeMillis() // 采样时间
				if (crt - time > 2 * 60 * 1000) {
					// 评估2 min的 CPU 平均值
					val avg = cpuUsage / i
					println("CPU平均使用率为 $avg %")
					if (avg > 80.0) {
						println("CPU 超载 (${ cpucount }%), 检查上一次警告时间以确认本次是否发送警报邮件")
						// 发送邮件后警报并不会解除, 所以需要设定一个时间段, 避免邮件轰炸
						// 由于解除警报最少需要2 min, 所以设置为4 min
						if (crt - lastSendMailTime > 4 * 60 * 1000) {
							// 发送邮件
							println("发送邮件 -- (${ Date() })")
							lastSendMailTime = crt
							sender.sendWarning(sendto, "CPU超负荷警告", "服务器CPU严重超载($avg%), 请管理员立即处理.\n$output\nFROM CPUWarning.")
						} else {
							println("距离上一次发送时间 ${ crt - lastSendMailTime }, 不发送邮件")
						}
					} else {
						println("警报解除 -- (${ Date() })")
						warning = false
					}
					// 评估结束后清空采样数据, 便于快速解除警报
					// 注意更新警戒时间, 避免采样不足就评估造成警报误解(当然, 这不过最多让CPU再猖獗几个party时间
					time = crt
					cpuUsage = 0.0
					i = 0
				}
			} else {
				println("当前处于安全状态(CPU $cpucount %) -- (${ Date() })\n$output")
			}
			
			// 休眠是必须的, 无论是否警戒状态, 不过警戒状态下没那么多party时间
			Thread.sleep(if (warning) warningTime else partyTime)
		} catch(e: Throwable) {
			e.printStackTrace()
		}
	}
}
