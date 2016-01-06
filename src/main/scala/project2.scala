
import akka.actor._
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.math._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.naming.Name



case class init(minions: ListBuffer[ActorRef], neighbors: ListBuffer[Int])
case class sendRumor()
case class processRumor()
case class receiveRumor()
case class receiveMsg(s: Double, w: Double)
case class processPushSum(s: Double, w: Double)
case class sendMsg(s: Double, w: Double)
case class updateNeighbours(minionName: String)
case class startSystem()
case class checkConvergence()
case class terminateSystem()

object project2 {

  def main(args: Array[String]) {

    var numNodes: Int = args(0).toInt
    var topology: String = args(1).toString()
    var algo: String = args(2).toString()
    var minionsList: ListBuffer[ActorRef] = new ListBuffer()
    var neighboursList: ListBuffer[Int] = null

    if (args.length != 3) {
      println("invalid # of arguments passed!")
      System.exit(1) //abnormal termination
    }

    val actorSystem = ActorSystem("AsynchronousGossip")
    val minionHandler = actorSystem.actorOf(Props(new MinionHandler(numNodes)), name = "minionmaster")
    if (topology == "3D" || topology == "imp3D") {
      //change numNodes
      var rValue: Int = (cbrt(numNodes).toDouble).ceil.toInt
      numNodes = rValue * rValue * rValue
    }
    println("program running on a '" + topology + "' topology that has '" + numNodes + "' nodes communicating through '" + algo + "' algorithm")
    for (i <- 0 until numNodes) {
      minionsList.append(actorSystem.actorOf(Props(new Minion(numNodes, minionHandler)), name = i.toString))
      neighboursList = getList(i, numNodes, topology)
      minionsList(i) ! init(minionsList, neighboursList)
    }
    var randNode = Random.nextInt(numNodes)
    //Start time calculation for algorithm
    minionHandler ! startSystem()
    if (algo == "gossip")
      minionsList(randNode).tell(receiveRumor(), minionHandler)
    else if (algo == "pushsum")
      minionsList(randNode).tell(receiveMsg(randNode, 0), minionHandler)
    else {
      println("please pass appropriate algorithm:-[gossip or pushsum]")
      System.exit(1)
    }
  }

  def getList(i: Int, n: Int, topology: String): ListBuffer[Int] = {
    var neighbours: ListBuffer[Int] = ListBuffer()

    if (topology == "full") {
      for (j <- 0 until n) {
        if (i != j)
          neighbours.append(j)
      }
    }

    if (topology == "line") {
      if ((i - 1) >= 0) {
        neighbours.append(i - 1)
      }
      if ((i + 1) < n) {
        neighbours.append(i + 1)
      }
    }
    if (topology == "3D" || topology == "imp3D") {
      var k = cbrt(n).toInt
      var level = i / (k * k)
      var upperlimit = (level + 1) * k * k
      var lowerlimit = level * k * k
      if ((i - k) >= lowerlimit) {                      //up
        neighbours.append(i - k)
      }
      if ((i + k) < upperlimit) {                       //down
        neighbours.append(i + k)
      }
      if ((((i - 1) % k) != (k - 1)) && (i - 1) >= 0) { //left
        neighbours.append(i - 1)
      }
      if (((i + 1) % k) != 0) {                         //right
        neighbours.append(i + 1)
      }
      if ((i + (k * k) < n)) {                          //front
        neighbours.append(i + (k * k))
      }
      if (i - (k * k) >= 0) {                           //back
        neighbours.append(i - (k * k))
      }
      if (topology == "imp3D") {                       //extra node for imp3D
        var randomNode: Int = Random.nextInt(n)
        while (neighbours.contains(randomNode)) {
          randomNode = Random.nextInt(n)
        }
        neighbours.append(randomNode)
      }
    }
    neighbours
  }
}
class Minion(numNodes: Int, minionHandler: ActorRef) extends Actor with ActorLogging {

  var minionRefList: ListBuffer[ActorRef] = new ListBuffer()
  var neighboursList: ListBuffer[Int] = new ListBuffer()
  var arbVal: Int = 10
  var msgsCount: Int = 0
  //initializing S=node number
  var S: Double = (self.path.name).toInt
  var W: Double = 1
  var prevratio: Double =0
  var currentratio: Double =0
  var b: Boolean=true
  var tick: Boolean=true
  var x:Double=0
  
  def receive = {
    case init(minions, neighbors) => {
      minionRefList = minions
      neighboursList = neighbors
    }
    case receiveRumor() => {
      //process received rumor fact
      self ! processRumor()
    }
    case processRumor() => {
      if (neighboursList.length > 0) {
        if (msgsCount < arbVal) {
          msgsCount += 1 //may be we need to move thus to receive
          //log.info("-" + self.path.name + "-neighbors->" + neighboursList)
           
        } else {
          //it means msgsRecevied >= arbVal leave the topology & update it
          if(msgsCount == arbVal) {
           // log.info(" message count reached at my node"+ msgsCount)
            msgsCount += 1
            //log.info("-" + self.path.name + "- I gossiped enough, bye... updating topology")
          for (j <- 0 until neighboursList.length)
            minionRefList(neighboursList(j)) ! updateNeighbours(self.path.name)
          minionHandler ! checkConvergence()
         }
        }
        //self ! sendRumor()
        if(tick){
        context.system.scheduler.schedule(0 milliseconds, 5 milliseconds, self, sendRumor())
        tick=false
        }
      } else if (neighboursList.length == 0){
                if(b){
                  //log.info(" i am left alone")
                b=false
                }
        minionHandler ! checkConvergence()
      }
      else
        log.info("-" + self.path.name + "-" + neighboursList)
    }
    case sendRumor() => {
      if (neighboursList.length > 0) {
        //pick neighbours
        var randneigh = neighboursList(Random.nextInt(neighboursList.size))
        minionRefList(randneigh) ! receiveRumor()
      }
    }
    case updateNeighbours(minionName) => {
      neighboursList = neighboursList.filter { x => x != minionName.toInt }
      //log.info("-" + self.path.name + "-updateNeighbours-" + neighboursList)
    }
    case receiveMsg(s, w) => {
      prevratio = S / W
      //log.info("-previousratio->" + prevratio)
      S = S + s
      W = W + w
      self ! processPushSum(S, W)
    }
    case processPushSum(s, w) => {
      currentratio = s / w
      //log.info("-currentratio->" + currentratio)
      x=abs(currentratio - prevratio)
      //log.info("-ratio difference->" + x)
      if (x < math.pow(10, -10))
        msgsCount += 1
      else
        msgsCount = 0 //reset counter
      
      if (msgsCount == 2){
        //log.info("-No s/w ratio change in ->" + msgsCount+" consecutive rounds")
        //log.info(" Converged at this node!")
        minionHandler ! terminateSystem()
      }
      //self ! sendMsg(s, w)
      if(tick){
      context.system.scheduler.schedule(0 milliseconds, 5 milliseconds, self, sendMsg(S, W))
      tick=false
      }
    }
    case sendMsg(s, w) => {
      S = s / 2
      W = w / 2
      //log.info("-[s,w]->[" + S + "," + W + "]")
      var randneigh = neighboursList(Random.nextInt(neighboursList.size))
      minionRefList(randneigh) ! receiveMsg(S, W)
    }
    case _ =>
      log.info("why am i here?")
  }
}

class MinionHandler(numNodes: Int) extends Actor with ActorLogging {
  var minionsgone: Int = 0
  var start:Long=0
  var end:Long=0
  var b:Boolean=true
  def receive = {
    case startSystem()=>{
      start=System.currentTimeMillis()
    }
    case checkConvergence() => {
      minionsgone += 1
      //log.info("so far, # of minions gone are :" + minionsgone)
      if (minionsgone == numNodes) {
        self ! terminateSystem()
      }
    }
    case terminateSystem() => {
      if(b){
      b=false
      end=System.currentTimeMillis()-start
      println("Total elapsed time:"+ end)
      println("shutting down...")
      context.system.shutdown()
      }
    }
    case _ =>
      log.info("why am i here?")
  }
}
