Brewday
=======
Brewday is a beer brewing app that is process-centric instead of recipe-centric.

The recipe-centric UX approach and data models of all other 
available beer brewing apps haven't really changed since ProMash. 

Brewday attempts to put the focus on process design, rather than recipe design. 
It aims to support building process flows impossible with current software.

Examples of processes impossible to represent in other brewing apps:
 * Parti-gyle brewing 
 * Splitting a batch at any stage, for example
     * Before the boil
     * After the boil
     * Before fermentation
 * Recombining any of the above after the are split and boiled, fermented, or otherwise processed
 * Freeze concentration after (or before) fermentation 
 * Arbitrarily complicated decoction mash schedules
 * Arbitrarily complicated fermentation schedules

Current status
-------------- 
Alpha. I have migrated off BeerSmith.

Running it
----------
* Grab the latest [release](https://github.com/alanmclachlan/brewday/releases).
* Unzip and execute brewday.exe (requires 64-bit Windows).

Key Features
------------
 * Process centric recipe design. Process steps can be combined in any way to 
 create processes of arbitrary complexity..
 * Explicit separation of recipes vs batches, including a 1-to-many relationship. 
 * Inventory management integrated with batches.
 * The backend is json files in local storage.
 
Roadmap
-------
 * Backend integration with popular cloud services.

Screenshots
-----------
**A generic all grain recipe featuring a step mash.**

![All grain step mash](all_grain.PNG)

**A decoction schedule**

![Decoction mash schedule](decoction.PNG)

**Water builder vanity shot, look at that LP**

![Water builder](waterbuilder.PNG)


