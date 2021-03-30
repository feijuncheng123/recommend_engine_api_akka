recommend_engine_api_akka
===
# 说明
1、基于scalatra+akka+redis+sparkml+tomcat的多线程异步rest算法推理接口。经测试的响应速率在100ms左右（特征数110个左右），并发100次压测中tps在30左右。<br />
2、接口中对akka actor部分做了修改，未经测试，仅作为参考。
