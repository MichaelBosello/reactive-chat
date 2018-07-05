#Reactive chat
1. Go to package **backend.run.cluster** and start **RunCassandra**
2. In the same package start one or more times all others main
3. Start all mains in **backend.run.service**, one or more times
4. Start one or more **ClientMain** from package **frontend**

 NB: You may need to leave a few seconds between start up.
  Hardcoded addresses are set for use all services in one machine,
  if you want to distribute clusters you need to update addresses in .conf file and in **utility.NetworkUtility**