package taller

/**
 * Taller 3 — consultas sobre un registro sísmico con expresiones for, mapas y
 * secuencias perezosas.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 */
class RegistroSismico {

  type Registro = List[Sismo]

  // Punto 1: consultas con expresiones for --------------------------------------

  /** Las regiones de los sismos que superan el umbral, en orden y con repetidos. */
  def fuertes(r: Registro, umbral: Double): List[String] = ???

  /** Las magnitudes registradas en una región. */
  def enRegion(r: Registro, region: String): List[Double] = ???

  /** Parejas de regiones distintas con algún sismo de magnitudes cercanas. */
  def paresCercanos(r: Registro, dif: Double): List[(String, String)] = {
    for {
      (a, i) <- r.map(_.region).distinct.zipWithIndex
      (b, j) <- r.map(_.region).distinct.zipWithIndex // distinct elimina regiones repetidas. zipWithIndex identifica
      // las tuplas con indices de posicion
      if i < j // i y j son los indices (ya que el for crea un producto cartesiano). la comparacion es para que no
      // se comparen dos regiones iguales o pares duplicados en orden inverso
      if r.exists(s1 => s1.region == a && // mientras que a y b se refiere a la primera y segunda region respectivamente
        r.exists(s2 => s2.region == b &&
          Math.abs(s1.magnitud - s2.magnitud) <= dif))
    } yield (a, b)
  }

  // Punto 2: agrupar con mapas --------------------------------------------------

  def porRegion(r: Registro): Map[String, List[Sismo]] = ???

  def conteoPorRegion(r: Registro): Map[String, Int] = ???

  def magnitudMaxima(r: Registro): Map[String, Double] = ???

  // Punto 3: consultas seguras --------------------------------------------------

  /** Un mapa que responde cero por las regiones que no están en el registro. */
  def conteoSeguro(r: Registro): Map[String, Int] = ???

  /** La magnitud máxima de una región, o vacío si no hay registros suyos. */
  def maximaDe(r: Registro, region: String): Option[Double] = ???

  /** La región con más sismos; en empate, la primera en orden alfabético. */
  def regionMasActiva(r: Registro): Option[String] = ???

  // Punto 4: réplicas perezosas -------------------------------------------------

  def redondear(x: Double): Double = Math.round(x * 100.0) / 100.0

  /** La secuencia infinita que empieza en la magnitud y multiplica por 0.8. */
  def replicas(magnitud: Double): LazyList[Double] =
    LazyList.iterate(redondear(magnitud))(x => redondear(x * 0.8))

  /** Las réplicas mientras superen el mínimo, incluida la primera. */
  def replicasSensibles(magnitud: Double, minimo: Double): List[Double] =
    replicas(magnitud).takeWhile(x => x > minimo).toList

  // Punto 5: informe combinado --------------------------------------------------

  /** Conteo, magnitud máxima y réplicas esperadas del sismo más fuerte, por región. */
  def informe(r: Registro, minimo: Double): Map[String, Resumen] = ???

}
