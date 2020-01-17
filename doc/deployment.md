## Deployment document



#### Development environment

| module | host | protocol | port | description  |
|:------:|:----:|:--------:|:----:|:------------:|
| RoomManager | super5 | TCP(HTTP) | 30387 | api |
| Processor   | super5 | TCP(HTTP) | 30388 | api |
| Distributor | super5 | TCP(HTTP) | 30389 | api |
| Distributor | super5 | TCP       | 30391 | 接收Processor流 |
| RtmpServer  | super5 | TCP(HTTP) | 30386 | api |
| RtpServer   | super3 | TCP(HTTP) | 30390 | api |
| Srs         | super3 | TCP(RTMP) | 62040 | rtmp |
| RtpServer   | super3 | UDP       | 61040 | rtp pull |
| RtpServer   | super3 | UDP       | 61041 | rtp push |
 
 


#### Production environment

| module | host | protocol | port | description  |
|:------:|:----:|:--------:|:----:|:------------:|
| RoomManager | super2 | TCP(HTTP) | 30387 | api |
| Processor   | super2 | TCP(HTTP) | 30388 | api |
| Distributor | super2 | TCP(HTTP) | 30389 | api |
| Distributor | super2 | TCP       | 30391 | 接收Processor流 |
| RtmpServer  | super2 | TCP(HTTP) | 30386 | api |
| RtpServer   | super1 | TCP(HTTP) | 30390 | api |
| Srs         | super1 | TCP(RTMP) | 62040 | rtmp |
| RtpServer   | super1 | UDP       | 61040 | rtp pull |
| RtpServer   | super1 | UDP       | 61041 | rtp push |
 
 




