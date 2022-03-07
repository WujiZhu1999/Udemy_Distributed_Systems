import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String ELECTION_NAMESPACE = "/election";
    //timeout for zookeeper to check its communication with the client
    private static final int SESSION_TIMEOUT = 3000;

    private ZooKeeper zooKeeper;
    private String currentZnodeName;

    public static void main(String args[]) throws IOException, InterruptedException, KeeperException {
        LeaderElection leaderElection = new LeaderElection();

        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.reeletLeader();

        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnected from Zookeeper, exiting application");
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);

        //only keep whatever is in c_...
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reeletLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);

            String smallestChild = children.get(0);

            if (smallestChild.equals(currentZnodeName)) {
                System.out.println("I am the leader");
                return;
            } else {
                System.out.println("I am not the leader");
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
            }
        }

        System.out.println("Watching znode " + predecessorZnodeName);



    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    /**
     * Since zooKeeper is event driven, the main thread which is created by:
     * --> main
     *    --> leaderElection.connectToZookeeper()
     * will stop immediately which have no time to response to connect event etc...
     * so will need the run method to hold the main thread
     */

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    //cleaning
    public void close() throws InterruptedException {
        //this will close the zooKeeper which still have event thread and io thread running
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event){
        switch (event.getType()){
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        //this function will wake up all threads that are in waiting stage
                        //which will make the main thread keep going
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                try {
                    reeletLeader();
                } catch (KeeperException e) {
                } catch (InterruptedException e) {
                }
        }

    }
}
