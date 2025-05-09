import java.util.LinkedHashMap;

public class Farmer implements Runnable {
    private final Simulation sim;
    private LinkedHashMap<String, Integer> animals_taken;
    private String currentState;
    private Field chosenField;
    private int breakTime;

    Farmer() {
        this.sim           = Simulation.getInstance();
        this.animals_taken = new LinkedHashMap<String, Integer>();
        this.currentState  = "AtEnclosure";
        this.chosenField   = null;
        this.breakTime     = (int) (2 * this.sim.farmerBreakRNG.nextDouble()
                                * Simulation.FARMER_BREAK_TICKS);
    }

    @Override
    public void run() {
        Logger.log("is born.");
        try {
            do { /* Farmer behaviour controller based on states. */
                if (Thread.currentThread().isInterrupted()) break;

                /* Farmer takes a break */
                if(this.sim.getTickCount() >= this.breakTime) {
                    Logger.log("Farmer is taking a break.");
                    Thread.sleep(Simulation.FARMER_BREAK_DURATION
                        * Simulation.MS_PER_TICK);
                    this.breakTime = this.sim.getTickCount()
                        + (int) (2 * this.sim.farmerBreakRNG.nextDouble()
                        * Simulation.FARMER_BREAK_TICKS);
                } else {
                    switch (this.currentState) {
                    case "AtEnclosure":
                        Logger.log(
                            Thread.currentThread().getName()
                            + " is at the Enclosure.");
                        this.atEnclosure();
                        break;
                    case "ChooseField":
                        Logger.log(
                            Thread.currentThread().getName()
                            + " is choosing a Field");
                        this.chooseField();
                        break;
                    case "AtField":
                        Logger.log(Thread.currentThread().getName()
                            + " is at the Field "
                            + this.chosenField.getFieldType());
                        this.atField();
                        break;
                    }
                }
            } while (true);
        } catch (InterruptedException e) {
            Logger.error("Interrupted. "
                + Thread.currentThread().getName()
                + " Farmer state: " + this.currentState);
        }
        /* Farmer lives till the simulation tells him to stop,
        *  so he never needs to declare himself dead. */
    }

    /* Check if can enter Enclosure and take as many animals as can. */
    public void atEnclosure() throws InterruptedException {
        if(canEnterEnclosure()) {
            animals_taken.putAll(
                sim.enclosure.takeAnimals(
                    Simulation.FARMER_CAPACITY));
            sim.enclosure.farmerLock.release();

            Logger.log(
                Thread.currentThread().getName()
                + " took animals from Enclosure.");
            this.currentState = "ChooseField";
        } else {
            Thread.sleep((Simulation.DELIVERY_SPAWN_TICKS / 4)
                * Simulation.MS_PER_TICK);
        }
    }

    private Boolean canEnterEnclosure() { 
        if(!sim.enclosure.isEmpty() && sim.enclosure.getDeliveryCount() == 0) {
            return sim.enclosure.farmerLock.tryAcquire();
        }
        return false;
    }

    /* Choose a Field of animal the Farmer has the most of. */
    public void chooseField() throws InterruptedException {
        if(this.animals_taken.isEmpty()) {
            this.currentState = "AtEnclosure";
            return;
        }
        String firstValue = this.animals_taken.entrySet().stream().findFirst()
            .get().getKey();
        this.chosenField = sim.getField(firstValue);

        Thread.sleep((10 + this.animals_taken.size()) * Simulation.MS_PER_TICK);
        this.currentState = "AtField";
    }

    /* Place animals in the Field if Farmer can enter.
    *  After that, either choose new Field to go to
    *  or wait at the current Field to enter. */
    public void atField() throws InterruptedException {
        if(canEnterField()) {
            this.chosenField.announceArival();
            placeAnimals();
            this.chosenField.farmerQueue.release();
        } else {
            return;
        }

        if(this.animals_taken.isEmpty()) {
            Logger.log(
                Thread.currentThread().getName()
                + " is going back to Enclosure.");
            Thread.sleep(10 * Simulation.MS_PER_TICK);
            this.currentState = "AtEnclosure";
        } else {
            this.currentState = "ChooseField";
        }
    }

    /* Check if Field is not full and has no Buyers waiting. */
    private Boolean canEnterField() throws InterruptedException {
        if((!this.chosenField.isFull() && this.chosenField.getBuyerCount() == 0)
                || this.chosenField.isEmpty()) {
            this.chosenField.farmerQueue.acquire();
            return true;
        } else {
            Logger.log(Thread.currentThread().getName()
                + " can't enter the Field " + this.chosenField.getFieldType());
            Thread.sleep(10 * Simulation.MS_PER_TICK);
            return false;
        }
    }

    /* Place as many animals as can in the current Field. */
    public void placeAnimals() throws InterruptedException {
        String animalType = this.chosenField.getFieldType();
        int animalsToPlace = this.animals_taken.get(animalType);
        if(this.chosenField.getAnimalCount() + animalsToPlace <= this.chosenField.getCapacity()) {
            this.animals_taken.remove(animalType);
        } else {
            animalsToPlace = this.chosenField.getCapacity() - this.chosenField.getAnimalCount();
            this.animals_taken.put(animalType, this.animals_taken.get(animalType) - animalsToPlace);
        }
        Logger.log("is placing " + animalsToPlace + " " + animalType + " in the Field");
        this.chosenField.putAnimals(animalsToPlace);
    }
}
