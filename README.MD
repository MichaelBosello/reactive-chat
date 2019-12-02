# Reactive chat

A reactive distributed chat with microservice architecture that takes into account non-functional aspects such as scalability, resilience and responsiveness. Each chat has the following qualities: the messages sent must be displayed in the same order by all users. The chat supports dynamic set of participants. Finally, A user can request to be the only one to write in the chat for a limited time. Used Akka cluster with sharding and persistence.

To ensure causal ordering among peers, I implementated the algorithm presented in M. Naimi and O. Thiare, "Distributed Mutual Exclusion Based on Causal Ordering", Journal of Computer Science 5.

## Run

1. Go to package **backend.run.cluster** and start **RunCassandra**
2. In the same package start one or more times all others main
3. Start all mains in **backend.run.service**, one or more times
4. Start one or more **ClientMain** from package **frontend**

 NB: You may need to leave a few seconds between start up.
  Hardcoded addresses are set for use all services in one machine,
  if you want to distribute clusters you need to update addresses in .conf file and in **utility.NetworkUtility**
