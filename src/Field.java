import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Field {
    private String fieldType;
    private int animalCount;
    private int capacity;
    private AtomicInteger farmersScheduled;

    public  Semaphore buyerQueue;
    public  Semaphore farmerQueue;
    private Boolean   isGettingFilled;
    private ReentrantLock lock;
    private Condition wakeUpBuyers;
    private Condition wakeUpFarmers;

    Field(String type, int capacity, int animalCount) {
        this.fieldType = type;
        this.capacity = capacity;
        if(animalCount > capacity) {
            this.animalCount = capacity;
        } else {
            this.animalCount = animalCount;
        }
        this.init();
    }

    private void init() {
        this.farmersScheduled = new AtomicInteger(0);
        this.isGettingFilled = false;
        this.buyerQueue  = new Semaphore(1, true);
        this.farmerQueue = new Semaphore(1, true);
        this.lock        = new ReentrantLock();
        this.wakeUpBuyers  = lock.newCondition();
        this.wakeUpFarmers = lock.newCondition();
        Logger.status(
            "Field Type: " + this.fieldType
            + " Animal Count: " + this.animalCount
            + " Capacity: " + this.capacity);
    }

    public String getFieldType() { return this.fieldType; }
    public int getAnimalCount() { return this.animalCount; }
    public int getAnimalsMissing() { return this.capacity - this.animalCount; }
    public int getCapacity() { return this.capacity; }
    public int getBuyerCount() { return this.buyerQueue.getQueueLength(); }
    public int getFarmerCount() { return this.farmerQueue.getQueueLength(); }
    public int getUrgency() {
        float fullness
            = 1
            - ( (float)this.getAnimalCount()
              / (float)this.getCapacity()    );

        // US = Urgency score
        int buyerUS     = this.getBuyerCount() * 10;
        int fullnessUS  = (int)Math.round(fullness * 100.0);
        int scheduledUS = this.farmersScheduled.get() * 15;
        int farmerUS    = this.getFarmerCount() * 50;


        int urgency_score
            = 100 // padding to avoid clamping-to-0 erasing variation
            + buyerUS
            + fullnessUS
            - scheduledUS
            - farmerUS;

        return urgency_score;
    }

    public synchronized Boolean isEmpty() { return this.animalCount == 0; }
    public synchronized Boolean isFull() { return this.animalCount == this.capacity; }

    public synchronized void announceVisit() { this.farmersScheduled.getAndIncrement(); }
    public synchronized void announceArival() { this.farmersScheduled.getAndDecrement(); }

    /* Put animals into the Field. */
    public void putAnimals(Integer animalCount) throws InterruptedException {
        lock.lock();
        try {
            Logger.log("trying to stock up...");
            while (!this.isGettingFilled
                   && this.getBuyerCount() > 0) {
                wakeUpFarmers.await();
            }

            this.animalCount += animalCount;

            if (this.animalCount == this.capacity) {
                isGettingFilled = false;
                wakeUpBuyers.signal();
            }
            Logger.log("added " + animalCount + " animals to field");

        } finally {
            lock.unlock();
        }
    }

    public synchronized void takeAnimal() throws InterruptedException {
        lock.lock();
        try {
            Logger.log("trying to take animal out");
            while (this.isGettingFilled
                   && this.getFarmerCount() > 0) {
                wakeUpBuyers.await();
            }

            this.animalCount -= 1;
            Logger.log(
                "An animal was bought from (" + this.fieldType + ") field.");
            Logger.status("New animal Count: " + this.animalCount);

            if (this.isEmpty()) {
                isGettingFilled = true;
                wakeUpFarmers.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
