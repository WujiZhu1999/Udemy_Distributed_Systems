import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class WatcherDemo implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String TARGET_ZNODE = "/target_znode";
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        WatcherDemo watcherDemo = new WatcherDemo();
        watcherDemo.connectToZookeeper();
        watcherDemo.watchTargetZnode();
        watcherDemo.run();
        watcherDemo.close();
    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
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

    public void watchTargetZnode() throws InterruptedException, KeeperException {
        Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
        if(stat == null){
            return;
        }

        byte [] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);
        System.out.println("Data : " + new String(data) + " children : " + children);
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
                System.out.println(TARGET_ZNODE + "was deleted");
                break;
            case NodeCreated:
                System.out.println(TARGET_ZNODE + "was created");
                break;
            case NodeDataChanged:
                System.out.println(TARGET_ZNODE + "data changed");
                break;
            case NodeChildrenChanged:
                System.out.println(TARGET_ZNODE + "children changed");
                break;
        }

        try {
            watchTargetZnode();
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }
    }
}
