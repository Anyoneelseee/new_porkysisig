package com.example.sisig

import NewStockDialogFragment
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.sisig.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryActivity : Fragment() {
    private lateinit var db: AppDatabase
    private var lastKnownOrderCount = 0
    private var totalServings = 0

    companion object {
        fun newInstance(): InventoryActivity {
            return InventoryActivity()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        db = AppDatabase.getInstance(requireContext())
        //db.productStockDao.deleteAll()
        //db.allOrderDao.deleteAll()


        // Initialize meat stock if it doesn't exist
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existingStock = db.productStockDao.getProductStockByName("Meat")
            if (existingStock == null) {
                // Using the correct ProductStock class from the database
                val initialStock = com.example.sisig.data.ProductStock(
                    productName = "Meat",
                    quantity = 50
                )

                db.productStockDao.insert(initialStock)

            }
        }

        // Observe stock changes
        viewLifecycleOwner.lifecycleScope.launch {
            db.productStockDao.getAllProductStock().collect { stocks ->
                val meatStock = stocks.find { it.productName == "Meat" }
                val displayLayout = view?.findViewById<LinearLayout>(R.id.display_added_stock)
                displayLayout?.removeAllViews() // Clear existing views
                meatStock?.let {
                    displayNewStock(it.productName, it.quantity.toString())
                }
            }
        }

        // Monitor orders and update stock
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.allOrderDao.getAllOrders().collect { orders ->
                    val currentOrderCount = orders.size
                    if (currentOrderCount > lastKnownOrderCount) {
                        val latestOrder = orders.last()
                        val totalServings = latestOrder.orderDetail.sumOf {
                            it.description.split("x")[0].trim().toInt()
                        }
                        decrementMeatStock(totalServings)
                    }
                    lastKnownOrderCount = currentOrderCount
                }
            }
        }

        return view
    }


    private fun decrementMeatStock(amount: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val meatStock = db.productStockDao.getProductStockByName("Meat")
            meatStock?.let {
                if (it.quantity >= amount) {
                    db.productStockDao.update(it.copy(quantity = it.quantity - amount))
                }
            }
        }
    }
    private fun updateStockDisplay() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val meatStock = db.productStockDao.getProductStockByName("Meat")
            withContext(Dispatchers.Main) {
                val displayLayout = view?.findViewById<LinearLayout>(R.id.display_added_stock)
                displayLayout?.removeAllViews()
                meatStock?.let {
                    displayNewStock(it.productName, it.quantity.toString())
                }
            }
        }
    }

    data class ProductStock(
        val name: String,
        var quantity: Int
    )

    private fun showNewStockDialog() {
        val dialog = NewStockDialogFragment()
        dialog.show(parentFragmentManager, "NewStockDialog")
    }

    private fun displayNewStock(productName: String, items: String) {
        val displayLayout: LinearLayout? = view?.findViewById(R.id.display_added_stock)

        displayLayout?.let {
            // Parent container to hold both row and separator
            val parentContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Create a row layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f
            }

            // Create and configure TextViews for productName, items, and category
            val productNameTextView = TextView(requireContext()).apply {
                text = productName
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }

            val itemsTextView = TextView(requireContext()).apply {
                text = items
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }



            // Create and configure the Edit button (using ImageButton for the icon)
            val editButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_edit_inventory_vector) // Set the Edit icon
                background = null // Make the background transparent
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    // Open the edit dialog
                }
            }

            // Create and configure the Delete button (using ImageButton for the icon)
            val deleteButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_delete_inventory_vector) // Set the Delete icon
                background = null // Make the background transparent
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    // Remove the parent container, which includes the row and separator
                    (parentContainer.parent as? ViewGroup)?.removeView(parentContainer)
                    Toast.makeText(requireContext(), "Stock Deleted", Toast.LENGTH_SHORT).show()
                }
            }

            // Add views to the row layout
            rowLayout.addView(productNameTextView)
            rowLayout.addView(itemsTextView)
            rowLayout.addView(editButton)
            rowLayout.addView(deleteButton)

            // Add the row layout to the parent container
            parentContainer.addView(rowLayout)

            // Add a separator below the row
            val separator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2 // Height of separator line
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            parentContainer.addView(separator)

            // Add the parent container to the display layout
            it.addView(parentContainer)
        }
    }


    private fun displayAlertStock(productName: String, alertAmount: String, items: String) {
        val displayLayout: LinearLayout? = view?.findViewById(R.id.display_alert_stock)

        displayLayout?.let {
            // Parent container to hold both row and separator
            val parentContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Create a row layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f // Adjust weight for dynamic alignment
            }

            // Create and configure TextViews for productName, alertAmount, and items
            val productNameTextView = TextView(requireContext()).apply {
                text = productName
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val alertAmountTextView = TextView(requireContext()).apply {
                text = alertAmount
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val itemsTextView = TextView(requireContext()).apply {
                text = items
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Create and configure the Edit button (using ImageButton for the icon)
            val editButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_edit_inventory_vector) // Set the icon
                background = null // Make the background transparent
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    // Open the edit dialog for the Stock Alert
                    showEditDialog(productNameTextView, alertAmountTextView, itemsTextView, isAlertStock = true)
                }
            }

            // Create and configure the Delete button (using ImageButton for the icon)
            val deleteButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_delete_inventory_vector) // Set the icon
                background = null // Make the background transparent
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    // Remove the parent container, which includes the row and separator
                    (parentContainer.parent as? ViewGroup)?.removeView(parentContainer)
                    Toast.makeText(requireContext(), "Alert Deleted", Toast.LENGTH_SHORT).show()
                }
            }

            // Add views to the row layout
            rowLayout.addView(productNameTextView)
            rowLayout.addView(alertAmountTextView)
            rowLayout.addView(itemsTextView)
            rowLayout.addView(editButton)
            rowLayout.addView(deleteButton)

            // Add the row layout to the parent container
            parentContainer.addView(rowLayout)

            // Add a separator below the row
            val separator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2 // Height of separator line
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            parentContainer.addView(separator)

            // Add the parent container to the display layout
            it.addView(parentContainer)
        }
    }



    private fun showEditDialog(productName: TextView, items: TextView, category: TextView, isAlertStock: Boolean = false) {
        // Inflate the appropriate dialog layout based on stock type
        val dialogView = if (isAlertStock) {
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_alert_stock_dialog_layout, null)
        } else {
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_stock_dialog_layout, null)
        }

        val productNameInput = dialogView.findViewById<EditText>(R.id.edit_product_name)
        val itemsInput = dialogView.findViewById<EditText>(R.id.edit_items)

        // If it's alert stock, we need to find the alert amount field as well
        val alertAmountInput = if (isAlertStock) {
            dialogView.findViewById<EditText>(R.id.edit_alert_amount)
        } else {
            null
        }

        // For non-alert stock, the category field will be populated as normal
        val categoryInput = dialogView.findViewById<EditText>(R.id.edit_category) // Make sure you have a category input in your layout

        // Populate the input fields with existing data
        productNameInput.setText(productName.text)
        itemsInput.setText(items.text)

        if (isAlertStock && alertAmountInput != null) {
            alertAmountInput.setText(category.text) // Reusing category as the alert amount in the stock edit view
        } else {
            categoryInput.setText(category.text) // For normal stock, populate category field
        }

        // Create the dialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialogBuilder.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Programmatically create buttons
        val buttonContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0) // Add some spacing
            }
        }

        // Create and style the Save button
        val saveButton = Button(requireContext()).apply {
            text = "Save"
            setBackgroundResource(R.drawable.rounded_textfield_confrim_btn) // Use your custom drawable
            setTextColor(ContextCompat.getColor(requireContext(), R.color.golden_yellow)) // Ensure the text is visible
            setPadding(24, 0, 24, 0)
            setOnClickListener {
                // Update the TextViews with new values
                productName.text = productNameInput.text.toString()
                items.text = itemsInput.text.toString()

                if (isAlertStock && alertAmountInput != null) {
                    category.text = alertAmountInput.text.toString() // Update alert amount
                } else {
                    category.text = categoryInput.text.toString() // Update category for normal stock
                }

                Toast.makeText(requireContext(), if (isAlertStock) "Alert Stock Updated" else "Stock Updated", Toast.LENGTH_SHORT).show()
                dialogBuilder.dismiss() // Close the dialog
            }
        }

        // Create and style the Cancel button
        val cancelButton = Button(requireContext()).apply {
            text = "Cancel"
            setBackgroundResource(R.drawable.rounded_cancel_btn) // Use your custom drawable
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            setPadding(24, 0, 24, 0)
            setOnClickListener {
                dialogBuilder.dismiss() // Close the dialog without saving
            }
        }

        // Add buttons to the container
        buttonContainer.addView(saveButton)
        buttonContainer.addView(cancelButton)

        // Add the button container to the dialog layout
        (dialogView as LinearLayout).addView(buttonContainer)

        // Show the dialog
        dialogBuilder.show()
    }

}
