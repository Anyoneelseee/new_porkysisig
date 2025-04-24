package com.example.sisig

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
import com.example.sisig.data.ProductStock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryActivity : Fragment() {
    private lateinit var db: AppDatabase
    private var lastKnownOrderCount = 0
    private val lowStockThreshold = 10 // Define low stock threshold

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

        // Initialize meat stock if it doesn't exist
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val existingStock = db.productStockDao.getProductStockByName("Meat")
            if (existingStock == null) {
                val initialStock = ProductStock(
                    productName = "Meat",
                    quantity = 50
                )
                db.productStockDao.insert(initialStock)
            }
        }

        // Observe stock changes
        viewLifecycleOwner.lifecycleScope.launch {
            db.productStockDao.getAllProductStock().collect { stocks ->
                val meatStock = stocks.find { stock -> stock.productName == "Meat" }
                val displayLayout = view.findViewById<LinearLayout>(R.id.display_added_stock)
                displayLayout?.removeAllViews()
                meatStock?.let { stock ->
                    displayNewStock(stock.productName, stock.quantity.toString())
                    // Display alert stock if quantity is low
                    if (stock.quantity <= lowStockThreshold) {
                        displayAlertStock(stock.productName, lowStockThreshold.toString(), stock.quantity.toString())
                    }
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
                        val totalServings = latestOrder.orderDetail.sumOf { detail ->
                            detail.description.split("x")[0].trim().toInt()
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
            meatStock?.let { stock ->
                if (stock.quantity >= amount) {
                    db.productStockDao.update(stock.copy(quantity = stock.quantity - amount))
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Insufficient stock for Meat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun displayNewStock(productName: String, items: String) {
        val displayLayout: LinearLayout? = view?.findViewById(R.id.display_added_stock)

        displayLayout?.let { layout ->
            // Parent container to hold both row and separator
            val parentContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Create a row layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3.5f
            }

            // Create and configure TextViews for productName and items
            val productNameTextView = TextView(requireContext()).apply {
                text = productName
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }

            val itemsTextView = TextView(requireContext()).apply {
                text = items
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Highlight low stock
            if (items.toIntOrNull() ?: 0 <= lowStockThreshold) {
                rowLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }

            // Create and configure the Edit button
            val editButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_edit_inventory_vector)
                background = null
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    showEditDialog(productNameTextView, itemsTextView, isAlertStock = false)
                }
            }

            // Add views to the row layout
            rowLayout.addView(productNameTextView)
            rowLayout.addView(itemsTextView)
            rowLayout.addView(editButton)

            // Add the row layout to the parent container
            parentContainer.addView(rowLayout)

            // Add a separator below the row
            val separator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            parentContainer.addView(separator)

            // Add the parent container to the display layout
            layout.addView(parentContainer)
        }
    }

    private fun displayAlertStock(productName: String, alertAmount: String, items: String) {
        val displayLayout: LinearLayout? = view?.findViewById(R.id.display_alert_stock)

        displayLayout?.let { layout ->
            // Parent container to hold both row and separator
            val parentContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Create a row layout
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3.5f
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

            // Create and configure the Edit button
            val editButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.icon_edit_inventory_vector)
                background = null
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                setOnClickListener {
                    showEditDialog(productNameTextView, itemsTextView, alertAmountTextView, isAlertStock = true)
                }
            }

            // Add views to the row layout
            rowLayout.addView(productNameTextView)
            rowLayout.addView(alertAmountTextView)
            rowLayout.addView(itemsTextView)
            rowLayout.addView(editButton)

            // Add the row layout to the parent container
            parentContainer.addView(rowLayout)

            // Add a separator below the row
            val separator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            parentContainer.addView(separator)

            // Add the parent container to the display layout
            layout.addView(parentContainer)
        }
    }

    private fun showEditDialog(
        productNameTextView: TextView,
        itemsTextView: TextView,
        alertAmountTextView: TextView? = null,
        isAlertStock: Boolean = false
    ) {
        // Inflate the appropriate dialog layout
        val dialogView = if (isAlertStock) {
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_alert_stock_dialog_layout, null)
        } else {
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_stock_dialog_layout, null)
        }

        val productNameInput = dialogView.findViewById<EditText>(R.id.edit_product_name)
        val itemsInput = dialogView.findViewById<EditText>(R.id.edit_items)
        val alertAmountInput = if (isAlertStock) {
            dialogView.findViewById<EditText>(R.id.edit_alert_amount)
        } else {
            null
        }

        // Populate the input fields
        productNameInput.setText(productNameTextView.text)
        itemsInput.setText(itemsTextView.text)
        alertAmountInput?.setText(alertAmountTextView?.text)
        productNameInput.isEnabled = false // Prevent editing product name

        // Create the dialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialogBuilder.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Create button container
        val buttonContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
        }

        // Create and style the Save button
        val saveButton = Button(requireContext()).apply {
            text = "Save"
            setBackgroundResource(R.drawable.rounded_textfield_confrim_btn)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.golden_yellow))
            setPadding(24, 0, 24, 0)
            setOnClickListener {
                // Validate inputs
                val newItems = itemsInput.text.toString().trim()
                val newAlertAmount = alertAmountInput?.text?.toString()?.trim()

                if (newItems.isEmpty() || (isAlertStock && newAlertAmount.isNullOrEmpty())) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val itemsInt = newItems.toIntOrNull()
                val alertAmountInt = newAlertAmount?.toIntOrNull()
                if (itemsInt == null || (isAlertStock && alertAmountInt == null)) {
                    Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (itemsInt < 0) {
                    Toast.makeText(requireContext(), "Quantity cannot be negative", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Update database
                lifecycleScope.launch(Dispatchers.IO) {
                    val stock = db.productStockDao.getProductStockByName(productNameTextView.text.toString())
                    stock?.let { productStock ->
                        db.productStockDao.update(productStock.copy(quantity = itemsInt))
                        withContext(Dispatchers.Main) {
                            // Update UI
                            itemsTextView.text = newItems
                            if (isAlertStock && alertAmountTextView != null) {
                                alertAmountTextView.text = newAlertAmount
                            }
                            Toast.makeText(
                                requireContext(),
                                if (isAlertStock) "Alert Stock Updated" else "Stock Updated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                dialogBuilder.dismiss()
            }
        }

        // Create and style the Cancel button
        val cancelButton = Button(requireContext()).apply {
            text = "Cancel"
            setBackgroundResource(R.drawable.rounded_cancel_btn)
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            setPadding(24, 0, 24, 0)
            setOnClickListener {
                dialogBuilder.dismiss()
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