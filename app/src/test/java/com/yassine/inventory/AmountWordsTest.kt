package com.yassine.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountWordsTest {

    @Test
    fun zero() {
        assertEquals("zéro dirham", AmountWords.inFrench(0.0))
    }

    @Test
    fun oneDirhamSingular() {
        assertEquals("un dirham", AmountWords.inFrench(1.0))
    }

    @Test
    fun twoDirhamsPlural() {
        assertEquals("deux dirhams", AmountWords.inFrench(2.0))
    }

    @Test
    fun tensWithEtUn() {
        assertEquals("vingt et un dirhams", AmountWords.inFrench(21.0))
    }

    @Test
    fun soixanteEtOnze() {
        assertEquals("soixante et onze dirhams", AmountWords.inFrench(71.0))
    }

    @Test
    fun quatreVingts() {
        assertEquals("quatre-vingts dirhams", AmountWords.inFrench(80.0))
    }

    @Test
    fun quatreVingtUn() {
        assertEquals("quatre-vingt-un dirhams", AmountWords.inFrench(81.0))
    }

    @Test
    fun deuxCents() {
        assertEquals("deux cents dirhams", AmountWords.inFrench(200.0))
    }

    @Test
    fun deuxCentTrenteHuit() {
        assertEquals("deux cent trente-huit dirhams", AmountWords.inFrench(238.0))
    }

    @Test
    fun milleCentCinquante() {
        assertEquals("mille cent cinquante dirhams", AmountWords.inFrench(1150.0))
    }

    @Test
    fun millions() {
        assertEquals(
            "un million deux cent trente-quatre mille cinq cent soixante-sept dirhams",
            AmountWords.inFrench(1_234_567.0),
        )
    }

    @Test
    fun centimesIncluded() {
        assertEquals(
            "mille trois cent trente-huit dirhams et soixante centimes",
            AmountWords.inFrench(1338.6),
        )
    }

    @Test
    fun roundingCleansFloatNoise() {
        assertEquals(
            "mille trois cent trente-huit dirhams et soixante centimes",
            AmountWords.inFrench(1338.5999999999),
        )
    }

    @Test
    fun sentencePrefix() {
        assertEquals(
            "ARRÊTÉE LA PRÉSENTE FACTURE À LA SOMME DE : MILLE TROIS CENT TRENTE-HUIT DIRHAMS ET SOIXANTE CENTIMES",
            AmountWords.sentence(1338.6),
        )
    }
}