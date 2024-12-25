import java

from FunctionCall fc
where fc.getArgument(0).toString().matches(".*token.*")
select fc, "Function call with 'token' keyword found."
