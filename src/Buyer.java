public class Buyer implements Runnable {
    private final Simulation sim;
    private Field targetField;
    private int spawnTick;
    
    Buyer() {
        this.sim = Simulation.getInstance();
        this.chooseField();
        this.spawnTick = sim.getTickCount();
    }

    @Override
    public void run() {
        Logger.log("is ready to buy.");
        try {
            do {
                Logger.log("wants to buy 1 " + this.targetField.getFieldType());
                if (Thread.currentThread().isInterrupted()) break;
                Thread.sleep(10 * Simulation.MS_PER_TICK);
            } while(!this.buy());
        } catch (InterruptedException e) {
            Logger.error("Buyer.buy() Interrupted.");
            this.sim.threadReportDeath(Thread.currentThread().getName(),
                                           false);
        }
        /* Die after buying. */
        sim.buyerWaitingTime.add(sim.getTickCount() - this.spawnTick);
        this.sim.threadReportDeath(Thread.currentThread().getName(),
                                           true);
    }

    private Boolean buy() throws InterruptedException {
        Boolean buyFlag = false;
        this.targetField.buyerQueue.acquire();
        if(!this.targetField.isEmpty()) {
            this.targetField.takeAnimal();
            buyFlag = true;
            sim.buyerMetrics.put(this.targetField.getFieldType(),
                sim.buyerMetrics.get(this.targetField.getFieldType()) + 1);
            Logger.log(this.targetField.getFieldType() + " bought successfully.");
        }
        this.targetField.buyerQueue.release();
        return buyFlag;
    }

    private void chooseField() {
        String animalToBuy = Simulation.ANIMAL_TYPES[this.sim.buyerPrefferenceRNG.nextInt(5)];
        this.targetField = sim.getField(animalToBuy);
    }
}
