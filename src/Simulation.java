import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

public class Simulation {
    private final static int
        NUMBER_OF_FARMERS    =    5;
    public final static int
        FARMER_CAPACITY      =   10;
    public final static int
        FARMER_BREAK_TICKS   =  200;
    public final static int
        FARMER_BREAK_DURATION = 150;
    private final static int
        BUYER_SPAWN_TICKS    =   10;
    public final static int
        DELIVERY_SPAWN_TICKS =  100;
    public final static int
        DELIVERY_SIZE        =   10;
    public final static int
        DAY_DURATION         = 1000;
    public final static int
        MS_PER_TICK          =   50; // Speed
    private final static int
        FIELD_CAPACITY_MAX   =   11; // Exclusive
    private final static int
        FIELD_CAPACITY_MIN   =   10; // Inclusive
    private final static int
        FIELD_STARTING_COUNT =    5;
    private final static long 
        RNG_SEED             =    0xFADD3D;
    private final static Boolean
        RNG_GENERATE_SEED    =    true; // Set true to use random seed.
    public final static String[]
        ANIMAL_TYPES = {"Chicken", "Pig", "Cow", "Sheep", "Llama"};

    private long   seed;
    public  Random spawnRNG;
    public  Random buyerPrefferenceRNG;
    public  Random deliveryRNG;
    public  Random farmerBreakRNG;

    private static Simulation instance;
    Enclosure enclosure;
    private AtomicInteger tickCounter;
    private HashMap<String, Thread> threads;
    private ArrayList<Field>  fields;

    /* METRICS */
    private int buyerCounter;
    private int deliveryCounter;
            ArrayList<Integer> buyerWaitingTime;
            ArrayList<Integer> buyerSpawnTime;
            HashMap<String, Integer> buyerMetrics;
            ArrayList<Integer> deliveryWaitingTime;
            ArrayList<Integer> deliverySpawnTime;
            HashMap<String, Integer> deliveryMetric;
            ArrayList<Integer> farmerStockTime;

    Simulation () {
        this.fields = new ArrayList<Field>();
        this.tickCounter   = new AtomicInteger(0);
        this.threads = new HashMap<String, Thread>();

        this.seed = RNG_SEED;
        if (RNG_GENERATE_SEED) {
            this.seed = (new Random()).nextLong();
        }
        this.spawnRNG            = new Random(this.seed    );
        this.buyerPrefferenceRNG = new Random(this.seed * 2);
        this.deliveryRNG         = new Random(this.seed * 3);
        this.farmerBreakRNG      = new Random(this.seed * 4);

        this.buyerCounter = 0;
        this.deliveryCounter = 0;
        this.buyerWaitingTime = new ArrayList<>();
        this.buyerSpawnTime = new ArrayList<>();
        this.deliveryWaitingTime = new ArrayList<>();
        this.deliverySpawnTime = new ArrayList<>();
        this.buyerMetrics = new HashMap<>();
        this.deliveryMetric = new HashMap<>();
        for(String animal : Simulation.ANIMAL_TYPES) {
            this.buyerMetrics.put(animal, 0);
            this.deliveryMetric.put(animal, 0);
        }
    }

    static public Simulation getInstance() {
        if (instance == null) {
            instance = new Simulation();
        }
        return instance;
    }

    public int getTickCount() { return this.tickCounter.get(); }

    public Field getField(String fieldType) {
        for(Field f : this.fields) {
            if(f.getFieldType() == fieldType) {
                return f;
            }
        }
        return null;
    }
    public LinkedList<String> getMostUrgentFields() {
        LinkedList<
            AbstractMap.SimpleImmutableEntry<
                String,
                Integer>
        > urgencyList = new LinkedList<>();

        // Collect urgencies from all fields
        for(Field f : this.fields) {
            urgencyList.add(
                new AbstractMap.SimpleImmutableEntry<String,Integer>
                    (f.getFieldType(),
                    f.getUrgency())
            );
        }

        // Sort in descending order
        Collections.sort(urgencyList,
            new Comparator<AbstractMap.SimpleImmutableEntry<String,Integer>>() {
                public int compare(
                    AbstractMap.SimpleImmutableEntry<String,Integer> a,
                    AbstractMap.SimpleImmutableEntry<String,Integer> b)
                {
                    return a.getValue().compareTo(b.getValue());
                }
            });

        LinkedList<String> urgentFields = new LinkedList<>();
        for(Map.Entry<String,Integer> p : urgencyList) {
            urgentFields.add(p.getKey());
        }

        return urgentFields;
    }

    public synchronized void threadReportBirth(String name, Runnable r) {
        if (this.threads.containsKey(name)) {
            Logger.error("thread with the name " + name + " was born already.");
            return;
        }

        Thread t = Thread.ofPlatform()
                         .name(name)
                         .unstarted(r);

        this.threads.put(name, t);
        t.start();
    }

    public synchronized void threadReportDeath(String name, Boolean naturalDeath) {
        if (!this.threads.containsKey(name)) {
            Logger.error(
                    "Cannot declare " + name + " dead. Thread was never born.");
            return;
        }

        try {
            Thread t = this.threads.get(name);
            if (t.isAlive()) {
                t.interrupt();
                t.join(500);
            }
        } catch (InterruptedException ie) {}

        if (naturalDeath) {
            this.threads.remove(name);
            return;
        }
        Logger.log(name + " was killed.");
    }

    public void init() {
        Logger.log("Initializing...");
        Logger.status("Seed Used: " + Long.toHexString(this.seed));
        this.createFields();
        this.createEnclosure();
        this.createFarmers();
    }

    private void createFields() {
        for(String animal : ANIMAL_TYPES) {
            Field field = new Field(animal,
            this.spawnRNG.nextInt(Simulation.FIELD_CAPACITY_MIN,
                Simulation.FIELD_CAPACITY_MAX),
                Simulation.FIELD_STARTING_COUNT);
            this.fields.add(field);
        }
    }

    private void createEnclosure() {
        this.enclosure = new Enclosure();
    }

    private void createFarmers() {
        Logger.log("Creating [" + NUMBER_OF_FARMERS + "] farmers.");
        for (Integer i = 0; i < NUMBER_OF_FARMERS; i++) {
            this.threadReportBirth("Farmer-" + (i+1), new Farmer());
        }
    }

    private void run() {
        int next_buyer_spawn_tick;
        int next_delivery_tick;

        Logger.log("Simulation starting...");

        next_delivery_tick    = 0;
        next_buyer_spawn_tick = 0;
        do {
            if (next_buyer_spawn_tick <= this.getTickCount()) {
                this.spawnBuyer();
                this.buyerSpawnTime.add(this.getTickCount());
                next_buyer_spawn_tick = this.getTickCount()
                    + (int) (2 * this.spawnRNG.nextDouble() * BUYER_SPAWN_TICKS);
            }

            if (next_delivery_tick <= this.getTickCount()){
                this.spawnDelivery();
                this.deliverySpawnTime.add(this.getTickCount());
                next_delivery_tick
                    = this.getTickCount()
                    + (int) (2 * this.spawnRNG.nextDouble() * DELIVERY_SPAWN_TICKS);
            }

            this.tickCounter.getAndIncrement();

            try {
                Thread.sleep(MS_PER_TICK);
            } catch (InterruptedException ie) {
                Logger.error(
                        "InterruptedException thrown by Thread.sleep()\n" + ie);
                break;
            }
        } while(this.getTickCount() < DAY_DURATION);
        Logger.log("Simulation ended...");
    }

    private void spawnBuyer() {
        this.threadReportBirth(
                "Buyer-" + this.buyerCounter,
                new Buyer());
        this.buyerCounter += 1;
    }

    private void spawnDelivery() {
        Delivery delivery = new Delivery();
        this.threadReportBirth(
                "Deliv-" + this.deliveryCounter,
                delivery);
        this.deliveryCounter += 1;
    }

    /* Kill all threads that are still alive. */
    private void terminate() throws InterruptedException {
        Thread.sleep(500);
        Logger.log("Cleaning up...");
        Iterator<Entry<String,Thread>> it = this.threads.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Thread> birthRecord = it.next();
            Logger.log("Sending terminate signal to " + birthRecord.getKey());
            this.threadReportDeath(birthRecord.getKey(),
                                    false);
            it.remove();
        }
        Logger.log("Done");
    }

    private float averageWaitTime(ArrayList<Integer> array) {
        int counter = 0;
        for(int i = 0; i < array.size(); i++) {
            counter += array.get(i);
        }
        return counter / array.size();
    }

    private float averageSpawnTime(ArrayList<Integer> array) {
        int counter = 0;
        for(int i = 0 ; i < array.size() - 1; i ++) {
            int tmpFirst = array.get(i+1);
            int tmpSecond = array.get(i);
            counter += tmpFirst - tmpSecond;
        }
        return counter / array.size();

    }

    private int countMap(HashMap<String, Integer> map) {
        int counter = 0;
        for(Integer value : map.values()) {
            counter += value;
        }
        return counter;
    }

    private void printMetrics() {
        Logger.log("Deliveries spawned: " + (this.deliveryCounter + 1));
        Logger.log("Average time for Delivery spawn: "
            + this.averageSpawnTime(this.deliverySpawnTime));
        Logger.log("Average Delivery wait time: "
            + this.averageWaitTime(this.deliveryWaitingTime));
        Logger.log("Total animals delivered: " + this.countMap(this.deliveryMetric));
        Logger.log("Enclosure metrics: " + this.deliveryMetric);
        Logger.log("Buyers spawned: " + (this.buyerCounter + 1));
        Logger.log("Average time for Buyer spawn: "
            + this.averageSpawnTime(this.buyerSpawnTime));
        Logger.log("Average Buyer wait time: "
            + this.averageWaitTime(this.buyerWaitingTime));
        Logger.log("Total animals bought: " + this.countMap(buyerMetrics));
        Logger.log("Animals bought: " + this.buyerMetrics);
    }
    public static void main(String[] args) throws InterruptedException {
        Simulation sim = Simulation.getInstance();
        sim.init();
        sim.run();
        sim.terminate();
        sim.printMetrics();
    }
}
