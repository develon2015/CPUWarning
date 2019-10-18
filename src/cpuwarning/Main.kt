package cpuwarning

import lib.process.Shell

fun main() {
	val sender = MailSender("zhangsan@develon.club", "pawd")
	val sh = Shell()
	sh.ready()

	val cmd = """top -bi -n 1"""
/*
	  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
	13785 root      20   0   23056    212      0 S   6.7  0.0   1:41.94 qemu-ga
*/
	
	while (true) {
		try {
			if (!sh.isAlive()) {
				println("Shell 进程 (PID: ${ sh.pid }) 失控, 请管理员调查")
				sh.ready()
			}
			val output = sh.run(cmd, 2000, 200) ?: ""
			println(output)
			val regex = """^.*\s+(\d+\.\d+)\s+(\d+.\d+)\s+.*$""".toRegex()
			// 匹配输出
			for (line in output.split('\n')) {
				val mr = regex.matchEntire(line)
				if (mr == null) continue
				val (cpu, mem) = mr.destructured
				println("$cpu $mem")
			}
			Thread.sleep(2000)
		} catch(e: Throwable) {
			e.printStackTrace()
		}
	}
}