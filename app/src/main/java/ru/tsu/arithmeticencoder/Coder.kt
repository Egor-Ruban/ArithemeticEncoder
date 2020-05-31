package ru.tsu.arithmeticencoder

import android.util.Log
import java.util.ArrayList

@OptIn(kotlin.ExperimentalUnsignedTypes::class)
object Coder {
    const val MAX_HEIGHT = 65535
    const val QUARTER = (MAX_HEIGHT+1)/4
    const val HALF = QUARTER * 2
    const val THREE_QUARTERS = QUARTER * 3

    var type = 1
    var size = 0
    var codedSize = 0
    lateinit var str : String
    fun compress(data : ByteArray){
        size = data.size
        CoderService.createNotification(size)
        var toCompress = data + ';'.toByte()
        Log.d("myLogs", " size ${toCompress.size}")
        val frequencyTable = countFrequencies(toCompress)
        val rangeTable = countRangeTable(frequencyTable)
        //println(rangeTable)
        for(item in rangeTable){
            print("${item.key}=(${item.value.first},${item.value.second}), ")
        }
        println()
        Log.d("myLogs", "length  ${toCompress.size}")
        //for(range in rangeTable){
        //    Log.d("myLogs_debug","coder ${range.key?.toChar()} ${range.value.first}, ${range.value.second}" )
        //}
        //Log.d("myLogs", "with type $type")
        var header = createHeader(frequencyTable, toCompress.size)
        encode(header, toCompress, rangeTable)
        val ratio = codedSize.toDouble()/data.size.toDouble()
        CoderService.sendLastCodeNotification(ratio)
        Log.d("myLogs", "ratio is $ratio")
    }

    private fun createHeader(frequencyTable: ArrayList<Pair<Byte, Int>>, size : Int) : ByteArray{
        var header = byteArrayOf()
        header = header.plus(type.toByte()) //сколько байт на частоту уходит
        header = header.plus((size shr 24).toByte()) //сохранения длины текста, потенциально большое
        header = header.plus((size shr 16).toByte()) // число, но в инт должно войти
        header = header.plus((size shr 8).toByte())
        header = header.plus((size).toByte())
        header = header.plus((frequencyTable.size.toByte()))
        when (type) {
            4 -> {
                for (pair in frequencyTable) {
                    header = header.plus(pair.first)
                    header = header.plus((pair.second shr 24).toByte())
                    header = header.plus((pair.second shr 16).toByte())
                    header = header.plus((pair.second shr 8).toByte())
                    header = header.plus((pair.second).toByte())
                }
            }
            3 -> {
                for (pair in frequencyTable) {
                    header = header.plus(pair.first)
                    header = header.plus((pair.second shr 16).toByte())
                    header = header.plus((pair.second shr 8).toByte())
                    header = header.plus((pair.second).toByte())
                }
            }
            2 -> {
                for (pair in frequencyTable) {
                    header = header.plus(pair.first)
                    header = header.plus((pair.second shr 8).toByte())
                    header = header.plus((pair.second).toByte())
                }
            }
            1 -> {
                for (pair in frequencyTable) {
                    header = header.plus(pair.first)
                    header = header.plus((pair.second).toByte())
                }
            }
        }
        //CoderService.sendHeader(header)
        codedSize += header.size
        return header
    }

    private fun encode(header : ByteArray, toEncode: ByteArray, rangeTable : Map<Byte?, Pair<Int, Int>>){
        for(item in rangeTable){
            print("${item.key}=(${item.value.first},${item.value.second}), ")
        }
        //println()
        str = ""
        var lowBorder = 0
        var highBorder = MAX_HEIGHT
        var symbolsEncoded = 0
        var bitsToDrop = 0
        val numberOfSymbols = toEncode.size
        var outputBytes = byteArrayOf(0)
        var currentByte = 0
        var indexByte = 1
        //println(highBorder)
        //println(numberOfSymbols)
        for(symbol in toEncode){
            symbolsEncoded++
            val oldLowBorder = lowBorder
            lowBorder = oldLowBorder +
                    rangeTable[symbol]!!.first*(highBorder - oldLowBorder + 1)/numberOfSymbols
            highBorder = oldLowBorder +
                    rangeTable[symbol]!!.second*(highBorder - oldLowBorder + 1)/numberOfSymbols - 1
            while(true){
                //if(output.second.size > 412 && output.second.size < 416){
                //    println(" ${output.second.lastIndex} ${symbol.toChar()}, $lowBorder, $highBorder")
                //    if(413 in output.second.indices) println(output.second[413].toUByte())
                //}
                if(highBorder < HALF){
                    if(currentByte in outputBytes.indices){
                        outputBytes[currentByte] = outputBytes[currentByte]
                        indexByte++
                        if(indexByte == 9){
                            outputBytes = outputBytes.plus(0)
                            currentByte++
                            indexByte = 1
                        }
                    }
                    while(bitsToDrop > 0){
                        if(currentByte in outputBytes.indices){
                            outputBytes[currentByte] = (outputBytes[currentByte] + (1 shl (8-indexByte))).toByte()
                            indexByte++
                            if(indexByte == 9){
                                outputBytes = outputBytes.plus(0)
                                currentByte++
                                indexByte = 1
                            }
                        }
                        bitsToDrop--
                    }
                } else if(lowBorder >= HALF){
                    outputBytes[currentByte] = (outputBytes[currentByte] + (1 shl (8-indexByte))).toByte()
                    indexByte++
                    if(indexByte == 9){
                        outputBytes = outputBytes.plus(0)
                        currentByte++
                        indexByte = 1
                    }
                    while(bitsToDrop > 0){
                        outputBytes[currentByte] = (outputBytes[currentByte] + 0).toByte()
                        indexByte++
                        if(indexByte == 9){
                            outputBytes = outputBytes.plus(0)
                            currentByte++
                            indexByte = 1
                        }
                        bitsToDrop--
                    }
                    lowBorder -= HALF
                    highBorder -= HALF
                    //Log.d("myLogs","2 ${symbol.toChar()}, $lowBorder, $highBorder ")
                    //Log.d("myLogs", "1")
                } else if((lowBorder >= QUARTER) and (highBorder < THREE_QUARTERS)){
                    bitsToDrop++
                    lowBorder -= QUARTER
                    highBorder -= QUARTER
                    //Log.d("myLogs","3 ${symbol.toChar()}, $lowBorder, $highBorder ")
                    //Log.d("myLogs", "btd $bitsToDrop")
                } else{
                    //println( "breakska $lowBorder, $highBorder")
                    //println("break ${lowBorder>=HALF}, ${highBorder<HALF}")
                    break
                }
                lowBorder += lowBorder
                highBorder += highBorder + 1
                //Log.d("myLogs","${symbol.toChar()}, $lowBorder, $highBorder ")
            }
        }
        println()
        println("coder")
        for(item in outputBytes){
            for( i in 1..8){
                print("${(item.toInt() shr (8-i)) and 1}")
            }
        }
        println()

        CoderService.sendHeader(header + outputBytes)
        codedSize += outputBytes.size
    }

    private fun addToOutput(
        bit : Char, bitsToDrop : Int, output : Pair<String, ByteArray>, symbolsEncoded : Int)
            : Pair<String, ByteArray>{
        var toDrop = bitsToDrop
        var newOutput = output.second
        val notBit = if(bit == '0') '1' else '0'
        print(bit)
        var newByteOutput = output.first + bit
        //str+=bit
        //Log.d("myLogs", "$bit")
        if(newByteOutput.length == 8){
            newOutput = addByteToOutput(newByteOutput, output.second, symbolsEncoded)
            newByteOutput = ""
        }
        while(toDrop > 0){
            //Log.d("myLogs", "$notBit")
            newByteOutput+=notBit
            //str+=notBit
            print(notBit)
            toDrop--
            if(newByteOutput.length == 8){
                newOutput = addByteToOutput(newByteOutput, output.second, symbolsEncoded)
                newByteOutput = ""
            }
        }
        return newByteOutput to newOutput
    }

    private fun addByteToOutput(newByteOutput : String, output: ByteArray, symbolsEncoded: Int)
            : ByteArray{
        var newByte  = 0
        for(digit in newByteOutput){
            newByte += newByte + digit.toByte() - '0'.toByte()
        }
        return output.plus(newByte.toByte())
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
        return rangeTable.toMap()
    }

    private fun countFrequencies(toEncode : ByteArray) : ArrayList<Pair<Byte, Int>> { // готово
        val fullFrequencyTable = IntArray(256) { 0 }
        for (symbol in toEncode) {
            fullFrequencyTable[symbol.toUByte().toInt()]++
        }   //подсчитывает частоты у всех байтов текста

        type = when (fullFrequencyTable.max()) {
            null -> 0
            in 0..256 -> 1
            in 256..16000 -> 2
            in 16000..16000000 -> 3
            else -> 4  //у войны и мир - 3
        }

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