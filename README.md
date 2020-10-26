Brewday
=======
 * Current status: alpha
 
JFX UI is progressing towards meeting my own needs to migrate off BeerSmith.

Running it
----------
* Grab the latest [release](https://github.com/alanmclachlan/brewday/releases).
* Unzip and execute brewday.exe (requires 64-bit Windows).

Manifesto
---------
The four key use cases to Brewday seeks improve on are:
 * Process centric recipe design.
 * Explicit separation of batches vs recipes.
 * Inventory management that is better integrated with batch design.
 * Backend integration with popular cloud services.

Process Centric
---------------
Brewday is a POC for beer brewing app that is process-centric instead of 
recipe-centric.

This project was born out of frustration with BeerSmith and other existing 
brewing apps. The recipe-centric UX approach and data models of every 
available product haven't really changed since ProMash. However arguably beer 
recipes are less important and interesting than the brewing process.

Brewday attempts to put the focus on process design, rather than recipe design. 
It aims to support building process flows impossible with current software, for
example splitting a batch multiple ways before or after the boil and tracking 
the output of all fermentations.

Screenshots
-----------
**A generic all grain recipe featuring a step mash.**

![All grain step mash](all_grain.PNG)

**A decoction schedule**

![Decoction mash schedule](decoction.PNG)

**Water builder vanity shot, look at that LP

![Water builder](waterbuilder.PNG)

Misc
----
Batches are explicitly separated from Recipes in a one-to-many relationship.

The backend is json files in local storage. An important part of the project 
vision is a variety of remote backend support including Dropbox, Google Drive 
and Github.
