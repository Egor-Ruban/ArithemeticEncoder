package ru.tsu.arithmeticencoder

import java.util.ArrayList

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
object Coder {
    const val MAX_HEIGHT = 65535
    const val QUARTER = (MAX_HEIGHT+1)/4
    const val HALF = QUARTER * 2
    const val THREE_QUARTERS = QUARTER * 3

    fun compress(data : ByteArray){
        val toEncode = "susiesaysitwaseasy" //сейчас строка, но вообще уже везде байты
        val frequencyTable = countFrequencies(toEncode.toByteArray())
        encode(toEncode.toByteArray(), countRangeTable(frequencyTable))
    }

    private fun encode(toEncode: ByteArray, rangeTable : Map<Byte?, Pair<Int, Int>>){
        var lowBorder = 0
        var highBorder = MAX_HEIGHT
        var symbolsEncoded = 0
        var bitsToDrop = 0
        val numberOfSymbols = toEncode.size
        var output = ""
        for(symbol in toEncode){
            symbolsEncoded++
            val oldLowBorder = lowBorder;
            lowBorder = oldLowBorder + rangeTable[symbol]!!.first * (highBorder - oldLowBorder + 1)/numberOfSymbols
            highBorder = oldLowBorder + rangeTable[symbol]!!.second * (highBorder - oldLowBorder + 1)/numberOfSymbols - 1
            print("$symbol, $lowBorder, $highBorder, ")
            while(true){
                if(highBorder < HALF){
                    output = addToOutput('0', bitsToDrop, output) //можно реализовать через расширение String
                    bitsToDrop = 0
                } else if(lowBorder >= HALF){
                    output = addToOutput('1', bitsToDrop, output) //аналогично
                    bitsToDrop = 0
                    lowBorder -= HALF
                    highBorder -= HALF
                } else if((lowBorder >= QUARTER) and (highBorder < THREE_QUARTERS)){
                    bitsToDrop++
                    lowBorder -= QUARTER
                    highBorder -= QUARTER
                } else break
                lowBorder += lowBorder
                highBorder += highBorder
            }
            println("$lowBorder, $highBorder, $output")
        }
    }

    private fun addToOutput(bit : Char, bitsToDrop : Int, output : String) : String{ //todo сделать по людски
        var toDrop = bitsToDrop // todo сделать через расширение String
        val notBit = if(bit == '0') '1' else '0'
        var newOutput = output + bit
        while(toDrop > 0){
            newOutput+=notBit
            toDrop--
        }
        return newOutput
    }

    private fun countRangeTable(frequencyTable : ArrayList<Pair<Byte, Int>>) //готова
            : Map<Byte?, Pair<Int, Int>>{ //создаем таблицу вида "символ? - (от - до)"
        val rangeTable = arrayListOf<Pair<Byte?, Pair<Int, Int>>>(null to (0 to 0))
        for(symbolPair in frequencyTable){
            rangeTable.add(
                symbolPair.first to (
                        rangeTable.last().second.second
                                to
                                rangeTable.last().second.second + symbolPair.second)
            )
        }
        println(rangeTable.toMap())
        return rangeTable.toMap()
    }

    private fun countFrequencies(toEncode : ByteArray) : ArrayList<Pair<Byte, Int>> { // готово
        val fullFrequencyTable = IntArray(256) { 0 }
        for (symbol in toEncode) {
            fullFrequencyTable[symbol.toUByte().toInt()]++
        }   //подсчитывает частоты у всех байтов текста

        val frequencyTable = arrayListOf<Pair<Byte, Int>>()
        for (byte in fullFrequencyTable.indices) {
            if (fullFrequencyTable[byte] > 0) {
                frequencyTable.add(byte.toByte() to fullFrequencyTable[byte])
            }
        } // создаем таблицу "байт - часота"
        frequencyTable.sortByDescending { it.second }
        return frequencyTable
    }

}