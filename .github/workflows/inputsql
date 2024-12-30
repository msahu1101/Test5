import javascript

/**
 * @name SQL injection
 * @description This query detects SQL injection vulnerabilities.
 * @kind problem
 * @problem.severity error
 * @id js/sql-injection
 */

class UserInput extends Expr {
  UserInput() {
    this = any(Expr e).(Variable v).getAnAccess().getAnArgument()
  }
}

class SqlQuery extends Expr {
  SqlQuery() {
    this = any(Expr e).(CallExpr c).getCallee().(MemberAccess ma).getMember().(PropertyAccess pa).getProperty().(Identifier i).getName() = "query"
  }
}

from UserInput input, SqlQuery query
where query.getAnArgument() = input
select query, "This SQL query uses user input directly, which can lead to SQL injection."
