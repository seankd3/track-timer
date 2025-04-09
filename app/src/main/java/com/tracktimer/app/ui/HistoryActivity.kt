package com.tracktimer.app.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.google.android.material.card.MaterialCardView
import com.tracktimer.app.R
import com.tracktimer.app.data.RouteRecord
import com.tracktimer.app.databinding.ActivityHistoryBinding
import com.tracktimer.app.utils.LocationUtils
import com.tracktimer.app.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.saved_routes)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Observe ViewModel
        observeViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { routeRecord ->
            showRouteDetailsDialog(routeRecord)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@HistoryActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun observeViewModel() {
        viewModel.allRouteRecords.observe(this) { records ->
            if (records.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(records)
            }
        }
    }

    private fun showRouteDetailsDialog(routeRecord: RouteRecord) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_route_details, null)
        
        // OSMDroid is configured in TrackTimerApplication class
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(routeRecord.routeName ?: "Route Details")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Delete") { _, _ ->
                showDeleteConfirmationDialog(routeRecord)
            }
            .create()
        
        // Set details
        dialogView.findViewById<TextView>(R.id.tvDate).text = 
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(routeRecord.startTime)
        
        dialogView.findViewById<TextView>(R.id.tvTime).text = 
            "Time: ${LocationUtils.formatElapsedTime(routeRecord.elapsedTimeMillis)}"
        
        dialogView.findViewById<TextView>(R.id.tvDistance).text = 
            "Distance: ${LocationUtils.formatDistance(routeRecord.distanceMeters)}"
        
        dialogView.findViewById<TextView>(R.id.tvAverageSpeed).text = 
            "Avg Speed: ${LocationUtils.formatSpeed(routeRecord.averageSpeedKmh)}"
        
        // Get the MapView and initialize it
        val mapView = dialogView.findViewById<MapView>(R.id.mapPreview)
        
        // Set basic map configuration
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false) // Usually not needed in a small preview
        }
        
        // Show the route on the map
        showRouteOnMap(mapView, routeRecord)
        
        dialog.show()
    }

    private fun showRouteOnMap(map: MapView, routeRecord: RouteRecord) {
        try {
            // Clear existing overlays
            map.overlays.clear()
            
            // Create GeoPoint objects for start and end points
            val startPoint = GeoPoint(routeRecord.startPointLat, routeRecord.startPointLng)
            val endPoint = GeoPoint(routeRecord.endPointLat, routeRecord.endPointLng)
            
            // Add start marker
            val startMarker = Marker(map).apply {
                position = startPoint
                title = "Start Point"
                icon = ContextCompat.getDrawable(this@HistoryActivity, R.drawable.ic_marker_start) ?: 
                       ContextCompat.getDrawable(this@HistoryActivity, android.R.drawable.ic_menu_mylocation)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(startMarker)
            
            // Add end marker
            val endMarker = Marker(map).apply {
                position = endPoint
                title = "End Point"
                icon = ContextCompat.getDrawable(this@HistoryActivity, R.drawable.ic_marker_end) ?: 
                       ContextCompat.getDrawable(this@HistoryActivity, android.R.drawable.ic_menu_myplaces)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(endMarker)
            
            // Draw line between points
            val routeLine = Polyline().apply {
                outlinePaint.color = ContextCompat.getColor(this@HistoryActivity, android.R.color.holo_blue_dark)
                outlinePaint.strokeWidth = 10f
                addPoint(startPoint)
                addPoint(endPoint)
            }
            map.overlays.add(routeLine)
            
            // Calculate the bounds that include both points
            val north = Math.max(startPoint.latitude, endPoint.latitude)
            val south = Math.min(startPoint.latitude, endPoint.latitude)
            val east = Math.max(startPoint.longitude, endPoint.longitude)
            val west = Math.min(startPoint.longitude, endPoint.longitude)
            
            // Add some padding
            val latPadding = (north - south) * 0.3
            val lonPadding = (east - west) * 0.3
            
            // Set bounds with padding
            val boundingBox = BoundingBox(
                north + latPadding,
                east + lonPadding,
                south - latPadding,
                west - lonPadding
            )
            
            // Move map to show both points
            map.zoomToBoundingBox(boundingBox, true, 100)
            map.invalidate() // Force redraw
            
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error showing route on map", e)
            // Show a fallback message if map fails to load
            Toast.makeText(this, "Unable to display route map", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(routeRecord: RouteRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete Route Record")
            .setMessage("Are you sure you want to delete this record? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRouteRecord(routeRecord)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Adapter for the route history RecyclerView
     */
    private inner class HistoryAdapter(
        private val onItemClick: (RouteRecord) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        
        private var routeRecords = emptyList<RouteRecord>()
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvTime: TextView = itemView.findViewById(R.id.tvTime)
            val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
            val tvSpeed: TextView = itemView.findViewById(R.id.tvSpeed)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_route_record, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val routeRecord = routeRecords[position]
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            
            holder.tvDate.text = dateFormat.format(routeRecord.startTime)
            holder.tvTime.text = LocationUtils.formatElapsedTime(routeRecord.elapsedTimeMillis)
            holder.tvDistance.text = LocationUtils.formatDistance(routeRecord.distanceMeters)
            holder.tvSpeed.text = LocationUtils.formatSpeed(routeRecord.averageSpeedKmh)
            
            holder.cardView.setOnClickListener {
                onItemClick(routeRecord)
            }
        }
        
        override fun getItemCount() = routeRecords.size
        
        fun submitList(list: List<RouteRecord>) {
            routeRecords = list
            notifyDataSetChanged()
        }
    }
}
