package com.example.sisig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sisig.R
import com.example.sisig.data.AllOrder
import com.example.sisig.data.AppDatabase
import com.example.sisig.data.MenuItemWithQuantity
import com.example.sisig.data.Order
import com.example.sisig.data.OrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderSummarySection: View
    private lateinit var confirmButton: Button
    private lateinit var db: AppDatabase
    private val selectedItems = mutableListOf<MenuItem>()

    companion object {
        fun newInstance(): HomeActivity {
            return HomeActivity().apply {
                // Optional: Set arguments here if needed
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        // Initialize RecyclerView and set its LayoutManager
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Inflate the order summary section dynamically
        orderSummarySection = LayoutInflater.from(requireContext())
            .inflate(R.layout.order_summary_section, view.findViewById(R.id.parent_layout), false)

        // Initialize confirmButton immediately after inflating orderSummarySection
        confirmButton = orderSummarySection.findViewById(R.id.confirm_order_button)

        // Add the inflated order summary section above the RecyclerView
        val parentLayout = view.findViewById<LinearLayout>(R.id.parent_layout)
        parentLayout.addView(orderSummarySection, 0)

        // Set up confirm button click listener
        confirmButton.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                saveOrder()
            } else {
                Toast.makeText(requireContext(), "Please select items first", Toast.LENGTH_SHORT).show()
            }
        }

        val menuItems = getSampleMenuItems()
        menuAdapter = MenuAdapter(menuItems) { menuItem ->
            selectedItems.add(menuItem)
            println("Selected Items: $selectedItems")
            updateOrderSummary()
            Toast.makeText(requireContext(), "Added ${menuItem.name} to cart!", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = menuAdapter
    }

    private fun saveOrder() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Group identical items and sum their quantities
                val itemCounts = selectedItems.groupingBy { it }.eachCount()

                // Calculate total amount considering quantities
                val totalAmount = itemCounts.entries.sumOf { (item, count) ->
                    item.price.toDouble() * count
                }

                // Create MenuItem objects with updated prices based on quantity
                val validatedItems = itemCounts.map { (item, count) ->
                    MenuItem(
                        item.name,
                        item.serving,
                        "${count}x ${item.description}",
                        item.price * count,  // Multiply price by quantity
                        item.imageResId
                    )
                }

                val allOrder = AllOrder(
                    orderDetail = validatedItems,
                    totalAmount = totalAmount  // This now reflects the quantity-adjusted total
                )

                db.allOrderDao.insert(allOrder)

                withContext(Dispatchers.Main) {
                    selectedItems.clear()
                    updateOrderSummary()
                    Toast.makeText(requireContext(), "Order saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OrderSave", "Error details: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving order: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateOrderSummary() {
        // Clear existing views in the order summary section
        val orderSummaryContainer = orderSummarySection.findViewById<LinearLayout>(R.id.order_summary_container)
        orderSummaryContainer.removeAllViews()

        // Iterate over selected items to create a new summary for each
        for (menuItem in selectedItems) {
            val orderItemView = LayoutInflater.from(requireContext()).inflate(R.layout.order_item, orderSummaryContainer, false)

            val orderItemName = orderItemView.findViewById<TextView>(R.id.order_item_name)
            val orderItemCost = orderItemView.findViewById<TextView>(R.id.total_cost)

            // Set the item name and cost in the summary
            orderItemName.text = menuItem.name
            orderItemCost.text = "${menuItem.price} PHP"  // Or you can calculate price based on quantity if needed

            // Add the new order item view to the summary container
            orderSummaryContainer.addView(orderItemView)
        }

        // Update the total amount at the bottom of the summary
        val totalAmount = selectedItems.sumOf { it.price.toDouble() }
        val totalAmountTextView = orderSummarySection.findViewById<TextView>(R.id.total_amount)
        totalAmountTextView.text = "Total: ${totalAmount} PHP"
    }


    private fun getSampleMenuItems(): List<MenuItem> {
        return listOf(
            MenuItem("Akin Lang", "(Single serving)", "Meat, Rice, and Egg", 65, R.drawable.menu_akinlang),
            MenuItem("Share Kami", "(Good for 2)", "2 packs, Meat, 2 packs, Rice, and 2 pcs, Egg", 75, R.drawable.menu_sharekami),
            MenuItem("Pang Barkada", "(Good for 3 to 4)", "4 packs, Meat, 4 packs, Rice, and 4 pcs, Egg", 100, R.drawable.menu_pangbarkada),
            MenuItem("Pang Pamilya", "(Good for 5 to 6)", "5 packs, Meat, 5 packs, Rice, and 5 pcs, Egg", 120, R.drawable.menu_pangpamilya)
        )
    }
}