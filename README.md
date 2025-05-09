# Farm - A Concurency Problem

This is a concurent programming module assignment, developed together with [Nino Candrlic](https://ie.linkedin.com/in/cptnemo).

To run the simulation, use:

> ./compile.sh

> ./run.sh

Design decisions and implementation details are explained [here](./assignment_description.pdf).

Sample simulation output can be found [here](./stdout.txt).

## Problem Specification:

A farm is open 24 hours a day, 365 days a year. The farm has multiple fields: pigs, cows, sheep, llamas, and chickens.
Time in the farm is measured in ticks. There are 1000 ticks in a day. Every 100 ticks (on average) a delivery is made of 10 animals, with a random number of animals for each of the above categories (totaling 10) e.g., 4 for pigs, 2 for cows, 1 for sheep, 2 for llamas, etc.

When a delivery of animals arrives, they are put into an enclosure, where a farmer who works in the farm can put (stock) the animals into their respective fields (assuming the farmer is not already busy). Only one person (farmer) can take animals from the enclosure at a time, and once taken cannot return them to the enclosure. Once they are finished another farmer can then take animals from the enclosure. Each farmer can move up to 10 animals at once, which could be a mix of any 10 animals e.g. 5 pigs, 3 sheep and 2 chickens. Only one farmer can be stocking a particular field at any one time (e.g. pigs). Once a farmer is stocking a field, any other arriving farmer(s) wishing to stock that field will need to wait. If a buyer arrives while a field is being stocked, they will need to wait for the farmer to finish stocking that field. A farmer may partly stock a field e.g. they only stock 3 of the 5 animals they are moving in that field (because it is now full). It takes a buyer 1 tick to take an animal from a field.

Every 10 ticks (on average) a buyer will buy an animal from one of the fields (randomly). If the field is empty, the buyer will wait until an animal for that field becomes available. This means there may be times where a particular field does not contain any animals, and the buyer will wait until an animal for that field is available.  

It takes a farmer 10 ticks to walk from where the deliveries arrive (the enclosure) to a particular field (e.g., to the field of cows), and 1 tick extra for every animal they are moving to that field. Additionally, for every animal they put in the field, it takes 1 tick. In this example, it would take 20 ticks for a farmer to move 10 animals to the field of cows, and another 10 ticks to stock that field with all 10 animals. If they return to the delivery area (the enclosure) where animals arrive from any field, it will take 10 ticks, and 1 tick extra for every animal they are still moving (i.e. animals they have not yet put into a field).

If a farmer is moving animals to stock multiple fields, it will take them 10 ticks to walk from one field to another field to begin stocking that field plus 1 tick for every remaining animal (to be stocked) that they are moving. For example, they may stock some cows first, and then move to another field. When they are finished, they return to the delivery area (the enclosure) to see if there are more animals to be stocked, if not, they wait. The journey from any field back to the delivery area where animals arrive takes 10 ticks. You can assume that it takes 0 ticks for a farmer to take animals from the enclosure. 

Note: Given that 100 animals arrive (on average) per day to the farm, and 100 animals will be bought (on average) per day by buyers, there will be times where some fields may not contain any animals. In these instances, buyers will need to wait for the animal to be available for that field. 


## Task:

Design a software system in Java to simulate the concurrent operation of the farm. Assume that animals are the resources, and the different activities are conducted in threads e.g., farmers, buyers, etc.