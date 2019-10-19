CPUWarning
===
监控CPU负载, 异常时邮件报警

> make && make run

```
服务器CPU严重超载(100.1840909090909%), 请管理员立即处理.
top - 00:53:27 up 36 days, 17:58,  5 users,  load average: 0.97, 0.39, 0.15
Tasks: 148 total,   1 running, 147 sleeping,   0 stopped,   0 zombie
%Cpu(s):  0.1 us,  0.0 sy,  0.0 ni, 99.8 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem :  2062096 total,   134600 free,   362592 used,  1564904 buff/cache
KiB Swap:   524284 total,   523508 free,      776 used.  1515476 avail Mem

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
  13211 d         20   0 3100664  34784  25480 S 100.0  1.7   2:02.85 java

FROM CPUWarning.
```
