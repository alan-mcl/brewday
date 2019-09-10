Brewday
=======
 * Current status: alpha

Running it
----------
* Requires Java 11 on the path.
* Extract the brewday zip file to a folder.
* Execute from that folder with run.cmd, or craft your own run.sh using the 
classpath located there.

Manifesto
---------
Brewday is a POC for beer brewing app that is process-centric instead of 
recipe-centric.

This project was born out of frustration with BeerSmith and other existing 
brewing apps. The recipe-centric UX approach and data models of every 
available product haven't really changed since ProMash. However ultimately beer 
recipes are less important and interesting than the brewing process.

Brewday attempts to put the focus on process design, rather than recipe design. 
It aims to support building process flows impossible with current software, for
example splitting a batch multiple ways before or after the boil and tracking 
the output of all fermentations.
 
The four key use cases to Brewday seeks improve on are:
 * Process centric recipe design.
 * Explicit separation of batches vs recipes.
 * Inventory management that is better integrated with batch design.
 * Backend integration with popular cloud services.

The POC front end is in Swing because I know the tech well and can knock it out
quickly. Long term I am considering JavaFX.

The backend is json files in local storage. An important part of the project 
vision is a variety of remote backend support including Dropbox, Google Drive 
and Github.
