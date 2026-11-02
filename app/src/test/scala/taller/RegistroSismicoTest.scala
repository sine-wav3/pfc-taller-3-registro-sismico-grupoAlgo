package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import Datos.registro

@RunWith(classOf[JUnitRunner])
class RegistroSismicoTest extends AnyFunSuite {

  val s = new RegistroSismico()
  import s._

  val vacio: List[Sismo] = List()

  // Punto 1: funcion fuertes -------------------------------------------------------------------

  test("fuertes: los que superan 5.0, en orden") {
    assert(fuertes(registro, 5.0) == List("Santander", "Choco", "Valle"))
  }

  test("fuertes: con un umbral alto no queda ninguno") {
    assert(fuertes(registro, 9.0) == List())
  }

  test("fuertes: conserva los repetidos") {
    assert(fuertes(registro, 3.0).length == 7)
  }

  test("fuertes: el umbral es estricto") {
    assert(!fuertes(registro, 6.0).contains("Choco"))
  }

  // Punto 1: funcion fuertes pruebas propuestas --------------------------------------------------

  test("fuertes: el umbral estricto deja fuera la magnitud mínima 3.1") {
    assert(fuertes(registro, 3.1).length == 6)
  }

  test("fuertes: con umbral 4.0 salen las regiones repetidas, en orden de aparición") {
    assert(fuertes(registro, 4.0) ==
      List("Valle", "Santander", "Choco", "Santander", "Valle"))
  }

  test("fuertes: sobre el registro vacío no hay regiones") {
    assert(fuertes(vacio, 1.0) == List())
  }

  test("fuertes: con umbral negativo salen todas las regiones en orden") {
    assert(fuertes(registro, -1.0) ==
      List("Valle", "Santander", "Valle", "Choco", "Santander", "Valle", "Choco"))
  }

  test("fuertes: con 5.2 solo salen Choco y Valle, en orden de aparición") {
    assert(fuertes(registro, 5.2) == List("Choco", "Valle"))
  }

  // Punto 1: función enRegion -------------------------------------------------------------------------------
  test("enRegion: las magnitudes del Valle") {
    assert(enRegion(registro, "Valle") == List(4.2, 3.8, 5.3))
  }

  test("enRegion: una región que no está da la lista vacía") {
    assert(enRegion(registro, "Amazonas") == List())
  }

  test("enRegion: sobre el registro vacío no hay magnitudes") {
    assert(enRegion(vacio, "Valle") == List())
  }

  // Punto 1: funcion enRegion pruebas propuestas ------------------------------------------------------

  test("enRegion: las magnitudes de Cauca en reg2") {
    assert(enRegion(registro, "Cauca") == List())
  }

  test("enRegion: las magnitudes del Choco conservan el orden del registro") {
    assert(enRegion(registro, "Choco") == List(6.0, 3.1))
  }

  test("enRegion: las magnitudes de Santander") {
    assert(enRegion(registro, "Santander") == List(5.1, 4.7))
  }

  test("enRegion: distingue mayúsculas y minúsculas") {
    assert(enRegion(registro, "valle") == List())
  }

  test("enRegion: la suma de las cantidades por región es el total de sismos") {
    val total = registro.map(_.region).distinct
      .map(reg => enRegion(registro, reg).length).sum
    assert(total == registro.length)
  }

  // Punto 1: funcion paresCercanos ---------------------------------------------------------------------

  test("paresCercanos: con 0.6 solo Valle y Santander") {
    assert(paresCercanos(registro, 0.6) == List(("Valle", "Santander")))
  }

  test("paresCercanos: con 1.0 salen las tres parejas, en orden de aparición") {
    assert(paresCercanos(registro, 1.0) ==
      List(("Valle", "Santander"), ("Valle", "Choco"), ("Santander", "Choco")))
  }

  test("paresCercanos: con 0.1 no hay ninguna") {
    assert(paresCercanos(registro, 0.1) == List())
  }

  test("paresCercanos: cada pareja sale una sola vez") {
    val ps = paresCercanos(registro, 1.0)
    assert(ps.map { case (a, b) => Set(a, b) }.distinct.length == ps.length)
  }

  test("paresCercanos: nunca empareja una región consigo misma") {
    assert(paresCercanos(registro, 5.0).forall { case (a, b) => a != b })
  }

  // Punto 1: funcion paresCercanos pruebas propuestas -------------------------------------------------------

  test("paresCercanos: con 0.8 Santander y Choco quedan fuera (difieren 0.9)") {
    assert(!paresCercanos(registro, 0.8).contains(("Santander", "Choco")))
  }

  test("paresCercanos: con una sola región no hay parejas") {
    val unaRegion = List(
      Sismo(Fecha(2024, 1), "Valle", 4.0, 10),
      Sismo(Fecha(2024, 2), "Valle", 4.1, 12)
    )
    assert(paresCercanos(unaRegion, 10.0) == List())
  }

  test("paresCercanos: con dif muy grande salen exactamente tres parejas") {
    assert(paresCercanos(registro, 10.0).length == 3)
  }

  test("paresCercanos: sobre el registro vacío no hay parejas") {
    assert(paresCercanos(vacio, 1.0) == List())
  }

  test("paresCercanos: con 2.0 Santander y Valle quedan fuera (difieren 2.5)") {
    assert(!paresCercanos(registro, 2.0).contains(("Santander", "Valle")))
  }


  // Punto 2 -------------------------------------------------------------------

  test("porRegion: agrupa los siete sismos en tres regiones") {
    assert(porRegion(registro).keySet == Set("Valle", "Santander", "Choco"))
    assert(porRegion(registro).values.map(_.length).sum == 7)
  }

  // Punto 2 porRegion pruebas propuestas ------------------------------------------------------

  test("porRegion: el registro vacío da el mapa vacío") {
    assert(porRegion(vacio) == Map())
  }

  test("porRegion: conserva el orden original dentro de cada grupo") {
    assert(porRegion(registro)("Valle") == List(
      Sismo(Fecha(2025, 3), "Valle", 4.2, 30),
      Sismo(Fecha(2025, 4), "Valle", 3.8, 25),
      Sismo(Fecha(2025, 5), "Valle", 5.3, 35)
    ))
  }

  test("porRegion: no incluye regiones que no aparecen en el registro") {
    val r = List(Sismo(Fecha(2026, 1), "Meta", 2.8, 15))
    assert(!porRegion(r).contains("Amazonas"))
    assert(!porRegion(r).contains("Antioquia"))
  }

  test("porRegion: conserva el contenido exacto de los sismos por región") {
    val s1 = Sismo(Fecha(2026, 1), "Santander", 2.0, 5)
    val s2 = Sismo(Fecha(2026, 2), "Santander", 2.5, 8)
    val r = List(s1, s2)
    assert(porRegion(r)("Santander") == List(s1, s2))
  }

  test("porRegion: conserva el orden original de los sismos en cada región") {
    // Valida que la agrupación no altere el orden de llegada de los sismos
    val resultado = porRegion(registro)
    assert(resultado("Valle").map(_.magnitud) == List(4.2, 3.8, 5.3))
    assert(resultado("Choco").map(_.profundidad) == List(40, 20))
  }


  test("conteoPorRegion: tres, dos y dos") {
    assert(conteoPorRegion(registro) ==
      Map("Valle" -> 3, "Santander" -> 2, "Choco" -> 2))
  }

  test("magnitudMaxima: la mayor de cada región") {
    assert(magnitudMaxima(registro) ==
      Map("Valle" -> 5.3, "Santander" -> 5.1, "Choco" -> 6.0))
  }

  test("conteoPorRegion: el registro vacío da el mapa vacío") {
    assert(conteoPorRegion(vacio) == Map())
  }

  test("magnitudMaxima: las regiones ausentes no aparecen") {
    assert(!magnitudMaxima(registro).contains("Amazonas"))
  }

  // Punto 3 -------------------------------------------------------------------

  test("conteoSeguro: una región desconocida responde cero") {
    assert(conteoSeguro(registro)("Amazonas") == 0)
  }

  test("conteoSeguro: una región conocida responde su conteo") {
    assert(conteoSeguro(registro)("Valle") == 3)
  }

  test("conteoSeguro: sobre el registro vacío todo responde cero") {
    assert(conteoSeguro(vacio)("Valle") == 0)
  }

  test("maximaDe: el Chocó llega a 6.0") {
    assert(maximaDe(registro, "Choco") == Some(6.0))
  }

  test("maximaDe: una región sin registros da vacío") {
    assert(maximaDe(registro, "Amazonas") == None)
  }

  test("regionMasActiva: el Valle, con tres sismos") {
    assert(regionMasActiva(registro) == Some("Valle"))
  }

  test("regionMasActiva: sobre el registro vacío da vacío") {
    assert(regionMasActiva(vacio) == None)
  }

  test("regionMasActiva: en empate gana la primera alfabéticamente") {
    val empate = List(
      Sismo(Fecha(2025, 1), "Zulia", 4.0, 10),
      Sismo(Fecha(2025, 1), "Arauca", 4.0, 10))
    assert(regionMasActiva(empate) == Some("Arauca"))
  }

  // Punto 4 -------------------------------------------------------------------

  test("replicas: las primeras cuatro de un sismo de 5.0") {
    assert(replicas(5.0).take(4).toList == List(5.0, 4.0, 3.2, 2.56))
  }

  test("replicas: la secuencia es infinita y no se cuelga") {
    assert(replicas(5.0).take(200).toList.length == 200)
  }

  test("replicas: empieza en la magnitud dada") {
    assert(replicas(7.3).head == 7.3)
  }

  test("replicas: los valores van redondeados a dos decimales") {
    assert(replicas(5.0).take(6).toList.forall(x => x == Math.round(x * 100.0) / 100.0))
  }

  test("replicasSensibles: las que superan 3.0 partiendo de 5.0") {
    assert(replicasSensibles(5.0, 3.0) == List(5.0, 4.0, 3.2))
  }

  test("replicasSensibles: si la primera no supera el mínimo, no hay ninguna") {
    assert(replicasSensibles(2.0, 3.0) == List())
  }

  test("replicasSensibles: el mínimo es estricto") {
    assert(replicasSensibles(3.0, 3.0) == List())
  }


  // Punto 4: Pruebas propuestas------------------------------------------------

  test("replicas: desde 10.0 salen 10.0, 8.0, 6.4") {
    assert(replicas(10.0).take(3).toList == List(10.0, 8.0, 6.4))
  }
  test("replicas: desde 1.0 el tercer elemento es 0.64") {
    assert(replicas(1.0)(2) == 0.64)
  }
  test("replicas: cada elemento es menor que el anterior") {
    val xs = replicas(8.0).take(10).toList
    assert(xs.zip(xs.tail).forall { case (a, b) => b < a })
  }
  test("replicasSensibles: desde 6.0 con mínimo 3.0") {
    assert(replicasSensibles(6.0, 3.0) == List(6.0, 4.8, 3.84, 3.07))
  }
  test("replicasSensibles: el mínimo estricto deja solo la primera") {
    assert(replicasSensibles(5.0, 4.0) == List(5.0))
  }

  // Punto 5 -------------------------------------------------------------------

  test("informe: el resumen de las tres regiones") {
    assert(informe(registro, 3.0) ==
      Map("Valle" -> Resumen(3, 5.3, 3),
          "Santander" -> Resumen(2, 5.1, 3),
          "Choco" -> Resumen(2, 6.0, 4)))
  }

  test("informe: el registro vacío da el mapa vacío") {
    assert(informe(vacio, 3.0) == Map())
  }

  test("informe: el conteo coincide con conteoPorRegion") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.sismos) } ==
      conteoPorRegion(registro))
  }

  test("informe: la máxima coincide con magnitudMaxima") {
    assert(informe(registro, 3.0).map { case (r, res) => (r, res.maxima) } ==
      magnitudMaxima(registro))
  }

  test("informe: las réplicas coinciden con replicasSensibles del más fuerte") {
    assert(informe(registro, 3.0)("Choco").replicas ==
      replicasSensibles(6.0, 3.0).length)
  }
}
