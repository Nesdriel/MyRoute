package com.myroute.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.myroute.MainActivity
import com.myroute.R
import com.myroute.dbmanager.DBManager
import com.myroute.models.Ruta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class FragmentMap : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i("MyRoute:Info", "Info puntp a")
        viewCont = inflater.inflate(R.layout.fragment_map, container, false)
        generateMap()
        return viewCont
    }

    companion object {
        const val ARG_PARAM1 = "param1"
        const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentMap().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        @SuppressLint("StaticFieldLeak")
        private lateinit var viewCont: View

        private lateinit var mapView: MapView
        private lateinit var dbManager : DBManager

        private fun generateMap(){
            // Buscando la etiqueta
            mapView = viewCont.findViewById(R.id.mapView)

            // Configuracion inicial
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.setBuiltInZoomControls(false)
            mapView.controller.setZoom(14.0)
        }

        fun generateRoute(context: MainActivity, id_route: String) : Boolean{
            dbManager = DBManager(context)
            val route : Ruta = dbManager.getRoute(id_route) ?: return false

            if (context.coroutineManager == null || context.coroutineManager!!.isCompleted){context.coroutineManager = context.lifecycleScope.async(
                Dispatchers.IO) {

                mapView.overlays.clear()
                mapView.invalidate()

                val roadManager = OSRMRoadManager(context , OSRMRoadManager.MEAN_BY_CAR)
                val road = roadManager.getRoad(route.getRefPoints())
                val roadOverlay = RoadManager.buildRoadOverlay(road, route.getColor(), 15F)

                mapView.overlays.add(roadOverlay)

                val nodeIcon = context.resources.getDrawable(R.drawable.icono_parada)
                for (i in route.getRefStops()!!.indices) {
                    val nodeMarker = Marker(mapView)
                    nodeMarker.position = route.getRefStops()!![i]
                    nodeMarker.icon = nodeIcon
                    nodeMarker.title = "Step $i"
                    mapView.overlays.add(nodeMarker)
                }

                mapView.invalidate()

                mapView.controller.setCenter(road.mBoundingBox.centerWithDateLine)
                mapView.controller.setZoom(14.0)

            }}else{return false}

            return true
        }
    }
}