import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Delivery implements Runnable {
    private final Simulation sim;
    private LinkedHashMap<String, Integer> delivery;
    private Integer spawnTick;

    Delivery() {
        this.sim = Simulation.getInstance();
        this.delivery = new LinkedHashMap<String, Integer>();
        this.spawnTick = sim.getTickCount();
    }

    @Override
    public void run() {
        this.generateDelivery();
        sim.enclosure.queueDelivery();
        try {
            sim.enclosure.deliveryLock.acquire();
            sim.enclosure.farmerLock.acquire();
            sim.enclosure.putDelivery(this.delivery);
            sim.enclosure.farmerLock.release();
            sim.enclosure.deliveryLock.release();
        } catch (InterruptedException e) {
            this.sim.threadReportDeath(
                Thread.currentThread().getName(),
                false);
            return;
        }
        sim.enclosure.removeDeliveryFromQueue();
        sim.deliveryWaitingTime.add(sim.getTickCount() - this.spawnTick);
        this.sim.threadReportDeath(
                    Thread.currentThread().getName(),
                    true);
    }

    private LinkedHashMap<String, Integer> generateDelivery() {
        ArrayList<String> deliveryTypes = generateAnimalTypeList();
        ArrayList<Integer> deliveryQuantities
            = generateAnimalCount(deliveryTypes.size());
        for(int i = 0; i < deliveryTypes.size(); i++) {
            this.delivery.put(deliveryTypes.get(i), deliveryQuantities.get(i));
        }
        Logger.log("Delivery generated: " + this.delivery.toString());
        return this.delivery;
    }

    /* Randomly select which types of animals will be in Delivery. */
    private ArrayList<String> generateAnimalTypeList() {
        int typeCount = this.sim.deliveryRNG.nextInt(Simulation.ANIMAL_TYPES.length) + 1;
        ArrayList<String> deliveryList = new ArrayList<String>();
        ArrayList<String> animalTypes = new ArrayList<String>(
            Arrays.asList(Simulation.ANIMAL_TYPES));
        for(int i = 0; i < typeCount; i++) {
            int tmp = this.sim.deliveryRNG.nextInt(animalTypes.size());
            deliveryList.add(animalTypes.get(tmp));
            animalTypes.remove(tmp);
        }
        return deliveryList;
    }

    /* Randomly select the amount of each type of animal in Delivery. */
    private ArrayList<Integer> generateAnimalCount(int typeCount) {
        int counter = 0;
        ArrayList<Integer> animalCountList = new ArrayList<Integer>();
        int upperBound = (Simulation.DELIVERY_SIZE + 1) - typeCount;

        for(int i = typeCount - 1; i != 0; i--) {
            int randomint = this.sim.deliveryRNG.nextInt(
                Math.max(1, upperBound - counter)) + 1;
            counter += randomint;
            animalCountList.add(randomint);
        }

        animalCountList.add(Simulation.DELIVERY_SIZE - counter);
        counter += animalCountList.getLast();
        return animalCountList;
    }
}
