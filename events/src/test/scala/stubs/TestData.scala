package stubs

import datatypes.Repository

object TestData {
  val ownedRepo: Repository    = Repository("DavidCorral94", "github-alerts-dummy")
  val notOwnedRepo: Repository = Repository("outr", "scribe")
}
