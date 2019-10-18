package cpuwarning

import lib.process.Shell
import java.util.Date

fun main() {
	val sender = MailSender("zhangsan@develon.club", "pawd")
	val sh = Shell()
	val cmd = """top -bi -n 1"""
	sh.ready()

	if (sh.isAlive()) {
		sh.run(cmd)
		if (sh.lastCode() == 0)
			println("Shell进程(${ sh.pid })正在保护您的CPU")
		else {
			println("您的计算机可能不支持top命令")
			sh.exit()
			return
		}
	} else {
		println("启动shell失败")
		return
	}

/*
	  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
*/
	
	val partyTime = 20000L // 非警戒状态的休闲时间/ms
	val warningTime = 200L // 警戒状态的取样时间
	var warning = false // 警戒状态?
	var time = 0L // 进入警戒状态的时间点
	var i = 0L // 采样次数
	var cpuUsage = 0L // CPU累积
	
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
			var cpucount = 0
			var memcount = 0
			for (line in output.split('\n')) {
				val mr = regex.matchEntire(line)
				if (mr == null) continue
				val (cpu, mem) = mr.destructured
				println("$cpu $mem")
				cpucount += cpu.toInt()
				memcount += mem.toInt()
			}
			
			// CPU异常, 如果没有戒备则进入警戒
			if (cpucount > 80.0) {
				if (!warning) { // 进入警戒模式, 记录时间点
					println("CPU $cpucount %, 进入警戒状态 (${ Date() })")
					warning = true
					i = 0
					cpuUsage = 0
					time = System.currentTimeMillis()
				}
			}
			
			// 警戒状态, 要么在达到底线时发送警报, 要么确认安全后解除警戒
			if (warning) {
				cpuUsage += cpucount
				i ++
				val crt = System.currentTimeMillis()
				if (crt - time > 2 * 60 * 1000) {
					// 评估2 min的 CPU 平均值
					val avg = cpuUsage / i
					println("CPU平均使用率为 $avg %")
					if (avg > 80.0) {
						println("CPU 超载 (${ cpucount }%), 发送警报邮件 (${ Date() })")
						lastSendMailTime = System.currentTimeMillis()
					} else {
						println("警报解除")
						warning = false
					}
				}
			}
			
			// 休眠是必须的, 无论是否警戒状态, 不过警戒状态下没那么多party时间
			Thread.sleep(if (warning) warningTime else partyTime)
		} catch(e: Throwable) {
			e.printStackTrace()
		}
	}
}