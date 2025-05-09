import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Enclosure {

    private final Simulation sim;
    private LinkedHashMap<String, Integer> animals;
    private int animalCount;
    private AtomicInteger deliveryCount;

    Semaphore farmerLock;
    Semaphore deliveryLock;

    Enclosure() {
        this.sim = Simulation.getInstance();
        this.animals = new LinkedHashMap<>();
        this.animalCount = 0;
        this.deliveryCount = new AtomicInteger(0);
        this.farmerLock = new Semaphore(1, true);
        this.deliveryLock = new Semaphore(1, true);
    }

    public int getDeliveryCount() { return this.deliveryCount.get(); }

    public Boolean isEmpty() { return this.animals.isEmpty(); }

    public synchronized
    LinkedHashMap<String, Integer> takeAnimals(
        int farmerCapacity
    ) {
        Logger.status("Animals in Enclosure: " + this.animals);
        LinkedHashMap<String, Integer> animalsGiven = new LinkedHashMap<>();

        // Rule 1: If the farmer can carry the entire enclosure, he should.
        if(this.animalCount <= Simulation.FARMER_CAPACITY) {
            animalsGiven.putAll(this.animals);
            this.animals.clear();
            this.animalCount = 0;

        // Rule 2-1: If the farmer needs to be selective, he picks which fields to
        //           stock based on urgency.
        } else {
            LinkedList<String> fieldCandidates = sim.getMostUrgentFields();
            Logger.status("Urgency list: " + fieldCandidates);

            // Rule 2-2: Farmer takes as much animals as he can carry.
            int freeSpace = farmerCapacity;
            do {
                String targetFieldName = fieldCandidates.pop();
                if (!this.animals.containsKey(targetFieldName)) {
                    continue;
                }
                int animalsOfTypeInEnclosure = this.animals.get(targetFieldName);
                Field targetField = sim.getField(targetFieldName);
                int fieldNeed = targetField.getAnimalsMissing();

                // Cannot take more than there is in enclosure.
                // Cannot take more than the field requires.
                // Cannot take more than he can carry.
                int animalCountToTake
                    = Math.min(
                        Math.min(
                            animalsOfTypeInEnclosure,
                            fieldNeed),
                      freeSpace);

                if (animalCountToTake == 0) continue;

                Logger.log(targetFieldName + " - Enclosure: " + animalsOfTypeInEnclosure
                                           + " Field need: " + fieldNeed
                                           + " Farmer free space: " + freeSpace);
                
                freeSpace -= animalCountToTake;
                animalsGiven.put(targetFieldName, animalCountToTake);
                targetField.announceVisit();

            } while (!fieldCandidates.isEmpty()
                     && !this.animals.isEmpty()
                     && freeSpace > 0);
        }

        Logger.log("Animals taken from enclosure:" + animalsGiven.toString());
        Logger.status("Animals in Enclosure: " + this.animalCount);

        return animalsGiven;
    }

    /* Take some animals from Enclosure when animalCount > Farmer carrying
     * capacity */
    public synchronized LinkedHashMap<String, Integer> takeSomeAnimals(int farmerCap) {
        LinkedHashMap<String, Integer> animalsGiven = new LinkedHashMap<>();
        Integer animalCounter = farmerCap;
        Iterator<Entry<String,Integer>> it = this.animals.entrySet().iterator();
        do {
            String key = this.animals.firstEntry().getKey();
            Integer value = this.animals.firstEntry().getValue();
            if(value <= animalCounter) {
                this.animals.remove(key);
            } else {
                this.animals.put(key, value - animalCounter);
                value = animalCounter;
            }
                animalsGiven.put(key, value);
                animalCounter -= value;
        } while(animalCounter != 0 && it.hasNext());
        return animalsGiven;
    }

    /* Put Delivery in the Enclosure. */
    public synchronized void putDelivery(LinkedHashMap<String, Integer> delivery) {
        Logger.log("Receiving Delivery.");
        Logger.status("Before: " + this.animals.toString());
        for(String animal : delivery.keySet()) {
            if(this.animals.containsKey(animal)) {
                int animalCount = this.animals.get(animal)
                    + delivery.get(animal);
                this.animals.put(animal, animalCount);
            }
            else {
                this.animals.put(animal, delivery.get(animal));
                
            }
            sim.deliveryMetric.put(animal, sim.deliveryMetric.get(animal)
                    + delivery.get(animal));
            sortEnclosure();
            countAnimals();
        }
        Logger.log("Delivery received.");
        Logger.status("After: " + this.animals.toString());
    }

    /* Sort all animals in Enclosure from most to least. */
    private void sortEnclosure() {
        List<Map.Entry<String, Integer>> entries
            = new ArrayList<>(this.animals.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a,
                Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        for(Map.Entry<String, Integer> entry: entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        this.animals = sorted;
    }

    private void countAnimals() {
        this.animalCount = 0;
        for(Integer value : this.animals.values()) {
            this.animalCount += value;
        }
    }

    public synchronized void queueDelivery() {
        this.deliveryCount.getAndIncrement();
    }

    public synchronized void removeDeliveryFromQueue() {
        this.deliveryCount.getAndDecrement();
    }
}
