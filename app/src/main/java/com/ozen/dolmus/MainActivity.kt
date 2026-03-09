package com.ozen.dolmus

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.ozen.dolmus.databinding.ActivityMainBinding
import java.util.Calendar
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
        const val MAX_DURAK_MESAFE_M = 400.0
    }

    private fun toMin(h: Int, m: Int) = h * 60 + m

    // ═══════════════════════════════════════════════════════════
    // GİDİŞ DURAKLARI — Özen Merkez → Seyhan Uygulama
    // ofset = Özen Merkez'den bu durağa tahmini süre (dakika)
    // Koordinatlar Google Maps güzergahından alınmıştır
    // ═══════════════════════════════════════════════════════════
    private val gidisDuraklar = listOf(
        Durak("Özen Merkez",             37.04012, 35.36215, ofset = 0),
        Durak("Z.Şalgamcı",              37.03985, 35.36330, ofset = 1),
        Durak("İMKB Lisesi",             37.03820, 35.36480, ofset = 2),
        Durak("Bey Mahallesi",           37.03650, 35.36610, ofset = 3),
        Durak("Z.Aktürk Market",         37.03510, 35.36730, ofset = 4),
        Durak("Demokrasi İlk Okulu",     37.03380, 35.36840, ofset = 5),
        Durak("A.Dut",                   37.03210, 35.36990, ofset = 6),
        Durak("A.Eçarşamba",             37.03050, 35.37120, ofset = 7),
        Durak("Özdemir Market",          37.02880, 35.37270, ofset = 8),
        Durak("B.A101",                  37.02720, 35.37420, ofset = 9),
        Durak("G.Hürriyet",              37.02550, 35.37580, ofset = 10),
        Durak("C.Şeker",                 37.02380, 35.37730, ofset = 11),
        Durak("Bey Mah. Çıkış",          37.02210, 35.37880, ofset = 12),
        Durak("Ulus Parkı",              37.01980, 35.38020, ofset = 13),
        Durak("Adliye",                  37.01820, 35.38180, ofset = 14),
        Durak("Teknosa",                 37.01650, 35.38330, ofset = 15),
        Durak("Z.Atilla Altıkat",        37.01480, 35.38480, ofset = 16),
        Durak("Çifte Minare",            37.01310, 35.38650, ofset = 17),
        Durak("D.Valilik",               37.01150, 35.38820, ofset = 18),
        Durak("D.Borsa Gidiş",           37.00980, 35.38990, ofset = 19),
        Durak("Karafatma Caddesi",       37.00810, 35.39160, ofset = 20),
        Durak("E.Karabucak",             37.00640, 35.39330, ofset = 21),
        Durak("G.Şırd Kadir",            37.00470, 35.39500, ofset = 22),
        Durak("Çukurova Elek. Işık",     37.00300, 35.39670, ofset = 23),
        Durak("Ali Sepici",              37.00130, 35.39840, ofset = 24),
        Durak("F.Doğ Prk Bim",          36.99960, 35.40010, ofset = 25),
        Durak("Z. Hastane",              36.99790, 35.40180, ofset = 26),
        Durak("Çukurova Kaymakamlığı",   36.99620, 35.40350, ofset = 27),
        Durak("F.Kurttepe Kvşk",         36.99450, 35.40520, ofset = 28)
    )

    // ═══════════════════════════════════════════════════════════
    // DÖNÜŞ DURAKLARI — Seyhan Uygulama → Özen Merkez
    // ofset = Y.Barbaros Geliş'ten bu durağa tahmini süre (dakika)
    // ═══════════════════════════════════════════════════════════
    private val donusDuraklar = listOf(
        Durak("O. Uygulama Çıkış",       36.98320, 35.41800, ofset = 0),
        Durak("Anadolu Lisesi Cad. 4B",  36.98490, 35.41630, ofset = 1),
        Durak("Anadolu Lisesi Cad. 2B",  36.98660, 35.41460, ofset = 2),
        Durak("Zeynel Abidin Cami",      36.98830, 35.41290, ofset = 3),
        Durak("J.Kırtasiye",             36.99000, 35.41120, ofset = 4),
        Durak("P. Doğal Park",           36.99170, 35.40950, ofset = 5),
        Durak("K.Adasa",                 36.99340, 35.40780, ofset = 6),
        Durak("Kenan Evren Blv.",        36.99510, 35.40610, ofset = 7),
        Durak("K.Turkuaz",               36.99680, 35.40440, ofset = 8),
        Durak("Y. Barbaros Geliş",       36.99850, 35.40270, ofset = 8),
        Durak("K.Işıklar",               37.00020, 35.40100, ofset = 9),
        Durak("D.Mehmet Akif Okulu",     37.00190, 35.39930, ofset = 10),
        Durak("L.Karadeniz",             37.00360, 35.39760, ofset = 11),
        Durak("L.Borsa Dönüş",           37.00530, 35.39590, ofset = 12),
        Durak("Çifte Minare Işık",       37.00700, 35.39420, ofset = 13),
        Durak("Atilla Altıkat Dönüş",    37.00870, 35.39250, ofset = 14),
        Durak("Küçük Saat",              37.01040, 35.39080, ofset = 15),
        Durak("Ulu Cami",                37.01210, 35.38910, ofset = 16),
        Durak("1.İnönü İlk Okulu",       37.01380, 35.38740, ofset = 17),
        Durak("M.Hur.Isık",              37.01550, 35.38570, ofset = 18),
        Durak("Hürriyet",                37.01720, 35.38400, ofset = 19),
        Durak("Y.Barbaros Geliş",        37.01890, 35.38230, ofset = 20)
    )

    // ── GİDİŞ sefer saatleri ────────────────────────────────
    private val seferlerHaftaici = listOf(
        toMin(5,35),
        toMin(6,0),toMin(6,8),toMin(6,16),toMin(6,24),toMin(6,32),
        toMin(6,40),toMin(6,45),toMin(6,50),toMin(6,55),
        toMin(7,0),toMin(7,5),toMin(7,10),toMin(7,15),toMin(7,20),
        toMin(7,25),toMin(7,30),toMin(7,35),toMin(7,40),toMin(7,45),toMin(7,50),toMin(7,55),
        toMin(8,0),toMin(8,6),toMin(8,12),toMin(8,18),toMin(8,24),toMin(8,30),toMin(8,36),toMin(8,42),toMin(8,48),toMin(8,54),
        toMin(9,0),toMin(9,6),toMin(9,12),toMin(9,18),toMin(9,24),toMin(9,30),toMin(9,36),toMin(9,42),toMin(9,48),toMin(9,54),
        toMin(10,0),toMin(10,6),toMin(10,12),toMin(10,18),toMin(10,24),toMin(10,30),toMin(10,36),toMin(10,42),toMin(10,48),toMin(10,54),
        toMin(11,0),toMin(11,6),toMin(11,12),toMin(11,18),toMin(11,24),toMin(11,30),toMin(11,36),toMin(11,42),toMin(11,48),toMin(11,54),
        toMin(12,0),toMin(12,6),toMin(12,12),toMin(12,18),toMin(12,24),toMin(12,30),toMin(12,36),toMin(12,42),toMin(12,48),toMin(12,54),
        toMin(13,0),toMin(13,6),toMin(13,12),toMin(13,18),toMin(13,24),toMin(13,30),toMin(13,36),toMin(13,42),toMin(13,48),toMin(13,54),
        toMin(14,0),toMin(14,6),toMin(14,12),toMin(14,18),toMin(14,24),toMin(14,30),toMin(14,36),toMin(14,42),toMin(14,48),toMin(14,54),
        toMin(15,0),toMin(15,6),toMin(15,12),toMin(15,18),toMin(15,24),toMin(15,30),toMin(15,36),toMin(15,42),toMin(15,48),toMin(15,54),
        toMin(16,0),toMin(16,6),toMin(16,12),toMin(16,18),toMin(16,24),toMin(16,30),toMin(16,36),toMin(16,42),toMin(16,48),toMin(16,54),
        toMin(17,0),toMin(17,6),toMin(17,12),toMin(17,18),toMin(17,24),toMin(17,30),toMin(17,36),toMin(17,42),toMin(17,48),toMin(17,54),
        toMin(18,0),toMin(18,10),toMin(18,20),toMin(18,30),toMin(18,40),toMin(18,50),
        toMin(19,0),toMin(19,10),toMin(19,20),toMin(19,30),toMin(19,40),toMin(19,50),
        toMin(20,0),toMin(20,10),toMin(20,20),toMin(20,30),toMin(20,40),toMin(20,50),
        toMin(21,0),toMin(21,10),toMin(21,20),toMin(21,30),toMin(21,40),toMin(21,50),
        toMin(22,0),toMin(22,10),toMin(22,20),toMin(22,35),toMin(22,50),toMin(23,5)
    )

    // ── DÖNÜŞ sefer saatleri ────────────────────────────────
    private val seferlerDonus = listOf(
        toMin(5,42),toMin(6,3),toMin(6,20),toMin(6,32),toMin(6,40),toMin(6,48),toMin(6,56),
        toMin(7,3),toMin(7,9),toMin(7,15),toMin(7,20),toMin(7,25),toMin(7,30),toMin(7,35),toMin(7,40),toMin(7,45),toMin(7,51),toMin(7,57),
        toMin(8,3),toMin(8,9),toMin(8,15),toMin(8,21),toMin(8,27),toMin(8,33),toMin(8,39),toMin(8,45),toMin(8,51),toMin(8,57),
        toMin(9,3),toMin(9,9),toMin(9,15),toMin(9,21),toMin(9,27),toMin(9,33),toMin(9,39),toMin(9,45),toMin(9,51),toMin(9,57),
        toMin(10,3),toMin(10,9),toMin(10,15),toMin(10,21),toMin(10,27),toMin(10,33),toMin(10,39),toMin(10,45),toMin(10,51),toMin(10,57),
        toMin(11,3),toMin(11,9),toMin(11,15),toMin(11,21),toMin(11,27),toMin(11,33),toMin(11,39),toMin(11,45),toMin(11,51),toMin(11,57),
        toMin(12,3),toMin(12,9),toMin(12,15),toMin(12,21),toMin(12,27),toMin(12,33),toMin(12,39),toMin(12,45),toMin(12,51),toMin(12,57),
        toMin(13,3),toMin(13,9),toMin(13,15),toMin(13,21),toMin(13,27),toMin(13,33),toMin(13,39),toMin(13,45),toMin(13,51),toMin(13,57),
        toMin(14,3),toMin(14,9),toMin(14,15),toMin(14,21),toMin(14,27),toMin(14,33),toMin(14,39),toMin(14,45),toMin(14,51),toMin(14,57),
        toMin(15,3),toMin(15,9),toMin(15,15),toMin(15,21),toMin(15,27),toMin(15,33),toMin(15,39),toMin(15,45),toMin(15,51),toMin(15,57),
        toMin(16,3),toMin(16,9),toMin(16,15),toMin(16,21),toMin(16,27),toMin(16,33),toMin(16,39),toMin(16,45),toMin(16,51),toMin(16,57),
        toMin(17,3),toMin(17,9),toMin(17,15),toMin(17,21),toMin(17,27),toMin(17,33),toMin(17,39),toMin(17,45),toMin(17,51),toMin(17,57),
        toMin(18,4),toMin(18,12),toMin(18,20),toMin(18,28),toMin(18,36),toMin(18,44),toMin(18,52),
        toMin(19,0),toMin(19,10),toMin(19,20),toMin(19,30),toMin(19,40),toMin(19,50),
        toMin(20,0),toMin(20,10),toMin(20,20),toMin(20,30),toMin(20,40),toMin(20,50),
        toMin(21,0),toMin(21,10),toMin(21,20),toMin(21,30),toMin(21,40),toMin(21,50),
        toMin(22,0),toMin(22,20),toMin(22,40),toMin(23,0)
    )

    private val hatlar by lazy {
        listOf(
            DolmusHat("GİDİŞ 🚐", "Özen Merkez → Seyhan Uygulama",
                R.color.ozen1_renk, seferlerHaftaici, "05:35", "23:05",
                varsayilanOfset = 23, duraklar = gidisDuraklar),
            DolmusHat("DÖNÜŞ 🔄", "Seyhan Uygulama → Özen Merkez",
                R.color.ozen2_renk, seferlerDonus, "05:42", "23:00",
                varsayilanOfset = 8, duraklar = donusDuraklar)
        )
    }

    private var secilenHatIndex = 0

    // ── Haversine mesafe (metre) ──────────────────────────────
    private fun mesafe(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun enYakinDurak(loc: Location, duraklar: List<Durak>): Pair<Durak, Double> =
        duraklar.map { d -> Pair(d, mesafe(loc.latitude, loc.longitude, d.lat, d.lon)) }
                .minByOrNull { it.second }!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupHatSecici()
        setupUpdateLoop()
        konumIzniIste()
    }

    private fun konumIzniIste() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST)
        } else {
            konumDinle()
        }
    }

    override fun onRequestPermissionsResult(rc: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(rc, perms, results)
        if (rc == LOCATION_PERMISSION_REQUEST &&
                results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            konumDinle()
        } else {
            binding.tvDurakBilgi.text = "📍 Konum izni yok — varsayılan durak"
        }
    }

    @SuppressLint("MissingPermission")
    private fun konumDinle() {
        // Son bilinen konum (hızlı)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            loc?.let { currentLocation = it; konumaBakarak() }
        }
        // Canlı güncellemeler (10sn'de bir)
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                r.lastLocation?.let { currentLocation = it; konumaBakarak() }
            }
        }
        fusedLocationClient.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    private fun konumaBakarak() {
        val loc = currentLocation ?: return
        val hat = hatlar[secilenHatIndex]
        val (durak, m) = enYakinDurak(loc, hat.duraklar)
        val mesafeMetin = if (m > 999) "${"%.1f".format(m/1000)} km" else "${m.toInt()} m"
        binding.tvDurakBilgi.text = when {
            m <= MAX_DURAK_MESAFE_M -> "📍 ${durak.ad} durağındasınız  (~${m.toInt()} m)"
            else                    -> "📍 En yakın: ${durak.ad}  ($mesafeMetin uzakta)"
        }
    }

    private fun aktifOfset(): Int {
        val loc = currentLocation
            ?: return hatlar[secilenHatIndex].varsayilanOfset
        val (durak, _) = enYakinDurak(loc, hatlar[secilenHatIndex].duraklar)
        return durak.ofset
    }

    private fun setupHatSecici() {
        binding.btnOzen1.setOnClickListener { hatSec(0) }
        binding.btnOzen2.setOnClickListener { hatSec(1) }
        binding.btnOzen3.visibility = View.GONE
        hatSec(0)
    }

    private fun hatSec(index: Int) {
        secilenHatIndex = index
        val hat = hatlar[index]
        binding.tvHatAdi.text = hat.ad
        binding.tvGuzergah.text = hat.guzergah
        binding.cardGeriSayim.setCardBackgroundColor(
            ContextCompat.getColor(this, hat.renk))
        binding.btnOzen1.alpha = if (index == 0) 1f else 0.4f
        binding.btnOzen2.alpha = if (index == 1) 1f else 0.4f
        konumaBakarak()
        guncelle()
    }

    private fun setupUpdateLoop() {
        updateRunnable = object : Runnable {
            override fun run() { guncelle(); handler.postDelayed(this, 1000L) }
        }
        handler.post(updateRunnable)
    }

    private fun guncelle() {
        val hat    = hatlar[secilenHatIndex]
        val ofset  = aktifOfset()
        val simdi  = Calendar.getInstance()
        val sd     = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        val ss     = simdi.get(Calendar.SECOND)

        val sonraki = hat.seferSaatleri.firstOrNull { (it + ofset) > sd }
        val onceki  = hat.seferSaatleri.lastOrNull  { (it + ofset) <= sd }

        if (sonraki != null) {
            val varis   = sonraki + ofset
            val kalanSn = (varis - sd) * 60 - ss
            val saat    = kalanSn / 3600
            val dk      = (kalanSn % 3600) / 60
            val sn      = kalanSn % 60

            binding.tvGeriSayimBuyuk.text =
                if (saat > 0) "%02d:%02d:%02d".format(saat, dk, sn)
                else          "%02d:%02d".format(dk, sn)

            val kalanDk = kalanSn / 60
            binding.tvKalanMetin.text = when {
                kalanDk <= 1 -> "⚡ Durağa geliyor!"
                kalanDk <= 3 -> "🏃 Hemen koş!"
                kalanDk <= 5 -> "🚶 Hazır ol"
                else         -> "Durağınıza ulaşmasına"
            }

            val ks = "%02d:%02d".format(sonraki / 60, sonraki % 60)
            val vs = "%02d:%02d".format(varis / 60, varis % 60)
            binding.tvSeferSaati.text = "Durağa varış: $vs  (kalkış: $ks)"

            if (onceki != null) {
                val aralik = (sonraki - onceki) * 60
                val gecen  = (sd - (onceki + ofset)) * 60 + ss
                binding.progressBar.progress =
                    ((gecen.toFloat() / aralik) * 100).toInt().coerceIn(0, 100)
            }
        } else {
            binding.tvGeriSayimBuyuk.text = "😴"
            binding.tvKalanMetin.text     = "Sefer tamamlandı"
            binding.tvSeferSaati.text     = "İlk sefer: ${hat.ilkSefer}"
            binding.progressBar.progress  = 100
        }

        val s3 = hat.seferSaatleri.filter { (it + ofset) > sd }.take(3)
        binding.tvSonrakiSeferler.text = s3.ifEmpty { null }
            ?.joinToString("   ") { s ->
                val v = s + ofset
                "%02d:%02d".format(v / 60, v % 60)
            } ?: "Bugünlük son sefer geçti"

        binding.tvSuankiSaat.text = "🕐 %02d:%02d:%02d".format(
            simdi.get(Calendar.HOUR_OF_DAY),
            simdi.get(Calendar.MINUTE),
            simdi.get(Calendar.SECOND))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}

// ── Veri sınıfları ───────────────────────────────────────────
data class Durak(
    val ad: String,
    val lat: Double,
    val lon: Double,
    val ofset: Int   // İlk duraktan bu durağa tahmini yolculuk süresi (dakika)
)

data class DolmusHat(
    val ad: String,
    val guzergah: String,
    val renk: Int,
    val seferSaatleri: List<Int>,
    val ilkSefer: String,
    val sonSefer: String,
    val varsayilanOfset: Int,
    val duraklar: List<Durak>
)
