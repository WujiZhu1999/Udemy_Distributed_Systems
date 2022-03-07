import cluster.management.LeaderElection;
import cluster.management.ServiceRegistry;
import org.apache.zookeeper.*;

import java.io.IOException;

public class Application implements Watcher{
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT =  3000;
    private static final int DEFALUT_PORT = 8080;
    private ZooKeeper zooKeeper;

    public static void main(String args[]) throws IOException, InterruptedException, KeeperException {
        int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFALUT_PORT;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZookeeper();

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, currentServerPort);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.reeletLeader();

        application.run();
        application.close();
        System.out.println("Disconnected from Zookeeper, exiting application");

    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return  zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        //this will close the zooKeeper which still have event thread and io thread running
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event){
        switch (event.getType()){
            case None:
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        //this function will wake up all threads that are in waiting stage
                        //which will make the main thread keep going
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
        }

    }
}
