@echo off
echo Running Inventory Panel Test...
call setenv.cmd
java -cp .;lib\* mclachlan.brewday.test.TestSwingInventoryPanel