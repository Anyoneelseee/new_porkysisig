package com.example.sisig

import android.annotation.SuppressLint
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
    private val selectedItems = mutableListOf<Pair<MenuItem, Int>>() // Store item and quantity

    companion object {
        fun newInstance(): HomeActivity {
            return HomeActivity()
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
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        orderSummarySection = LayoutInflater.from(requireContext())
            .inflate(R.layout.order_summary_section, view.findViewById(R.id.parent_layout), false)
        confirmButton = orderSummarySection.findViewById(R.id.confirm_order_button)

        val parentLayout = view.findViewById<LinearLayout>(R.id.parent_layout)
        parentLayout.addView(orderSummarySection, 0)

        confirmButton.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                saveOrder()
            } else {
                Toast.makeText(requireContext(), "Please select items first", Toast.LENGTH_SHORT).show()
            }
        }

        val menuItems = getSampleMenuItems()
        menuAdapter = MenuAdapter(menuItems) { menuItem, quantity ->
            selectedItems.add(Pair(menuItem, quantity))
            updateOrderSummary()
            Toast.makeText(requireContext(), "Added ${menuItem.name} x$quantity to cart!", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = menuAdapter
    }

    private fun saveOrder() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val itemCounts = selectedItems.groupBy { it.first }
                    .mapValues { entry -> entry.value.sumOf { it.second } }

                val totalAmount = itemCounts.entries.sumOf { (item, quantity) ->
                    item.price.toDouble() * quantity
                }

                val validatedItems = itemCounts.map { (item, quantity) ->
                    MenuItem(
                        item.name,
                        item.serving,
                        "${quantity}x ${item.description}",
                        item.price * quantity,
                        item.imageResId
                    )
                }

                val allOrder = AllOrder(
                    orderDetail = validatedItems,
                    totalAmount = totalAmount,
                    date = Date() // Set the current date
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

    @SuppressLint("MissingInflatedId")
    private fun updateOrderSummary() {
        val orderSummaryContainer = orderSummarySection.findViewById<LinearLayout>(R.id.order_summary_container)
        orderSummaryContainer.removeAllViews()

        // Group items to show total quantity per item
        val itemCounts = selectedItems.groupBy { it.first }
            .mapValues { entry -> entry.value.sumOf { it.second } }

        for ((menuItem, quantity) in itemCounts) {
            val orderItemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.order_item, orderSummaryContainer, false)

            val orderItemName = orderItemView.findViewById<TextView>(R.id.order_item_name)
            val orderItemQuantity = orderItemView.findViewById<TextView>(R.id.order_item_quantity)
            val orderItemCost = orderItemView.findViewById<TextView>(R.id.total_cost)

            orderItemName.text = menuItem.name
            orderItemQuantity.text = "x$quantity"
            orderItemCost.text = "${menuItem.price * quantity} PHP"

            orderSummaryContainer.addView(orderItemView)
        }

        val totalAmount = itemCounts.entries.sumOf { (item, quantity) ->
            item.price.toDouble() * quantity
        }
        val totalAmountTextView = orderSummarySection.findViewById<TextView>(R.id.total_amount)
        totalAmountTextView.text = "Total: $totalAmount PHP"
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