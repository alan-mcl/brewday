@echo off
echo Running Settings Node Test...
call setenv.cmd
java -cp .;lib\* mclachlan.brewday.test.TestSwingUiSettingsNode