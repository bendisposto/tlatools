-------------------------------- MODULE Base --------------------------------
VARIABLES clk
vars == <<clk>>

Init == clk = 0
        
Spec == Init /\ [][UNCHANGED vars]_vars

=============================================================================
