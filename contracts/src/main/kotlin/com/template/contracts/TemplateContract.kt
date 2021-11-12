package com.template.contracts

import com.template.states.TemplateState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class TemplateContract : Contract {

  companion object {

    // Used to identify our contract when building a transaction.
    const val ID = "com.template.contracts.TemplateContract"
  }

  // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
  // does not throw an exception.
  override fun verify(tx: LedgerTransaction) {
    // Verification logic goes here.
    val command = tx.commands.requireSingleCommand<Commands.Create>()
    when (command.value) {
      is Commands.Create -> requireThat {
        "No inputs should be consumed when sending the message.".using(tx.inputStates.isEmpty())
      }
    }
  }

  // Used to indicate the transaction's intent.
  interface Commands : CommandData {

    class Create : Commands
  }
}
