package ru.tsu.arithmeticencoder

import android.util.Log

object Decoder {
    const val MAX_HEIGHT = 65535
    const val QUARTER = (MAX_HEIGHT+1)/4
    const val HALF = QUARTER * 2
    const val THREE_QUARTERS = QUARTER * 3

    var currentByte = 0
    var size = 0

    fun decompress(inputText : ByteArray){ //перехуярить для байтов
        //считывание длины
        size = inputText.size
        CoderService.createNotification(inputText.size)
        var length : Int = inputText[1].toUByte().toInt()
        length = (length shl 8) + inputText[2].toUByte().toInt()
        length = (length shl 8) + inputText[3].toUByte().toInt()
        length = (length shl 8) + inputText[4].toUByte().toInt()

        Log.d("myLogs", "length read  $length")
        var frequencyTable = parseHeader(inputText)
        Log.d("myLogs", "header parsed")
        val rangeTable = countRangeTable(frequencyTable)
        Log.d("myLogs", "range table created")
        for(range in rangeTable){
            Log.d("myLogs_debug","decoder ${range.key?.toChar()} ${range.value.first}, ${range.value.second}" )
        }
        Log.d("myLogs", "with type ${inputText[0].toInt()}")
        decode(inputText, rangeTable, length)
    }

    private fun countRangeTable(frequencyTable : java.util.ArrayList<Pair<Byte, Int>>) //готова
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

    private fun parseHeader(inputText : ByteArray) : ArrayList<Pair<Byte, Int>>{
        when(inputText[0].toInt()){
            1 ->{
                return createFirstFrequencyTable(inputText)
            }
            2 ->{
                return createSecondFrequencyTable(inputText)
            }
            3 ->{
                return createThirdFrequencyTable(inputText)
            }
            4 ->{
                return createFourthFrequencyTable(inputText)
            }
            else -> return arrayListOf()
        }
    }

    private fun createFirstFrequencyTable(inputText: ByteArray) : ArrayList<Pair<Byte, Int>>{
        var frequencyTable : ArrayList<Pair<Byte, Int>> = arrayListOf()
        val symbols = inputText[5].toUByte().toInt()
        currentByte = 6
        var symbol: Byte = 0
        var freq: Int
        while (currentByte < 6 + 2 * symbols) {
            when (currentByte % 2) {
                0 -> symbol = inputText[currentByte]
                1 -> {
                    freq = inputText[currentByte].toUByte().toInt()
                    frequencyTable.add(symbol to freq)
                }
            }
            currentByte++
        }
        return frequencyTable
    }

    private fun createSecondFrequencyTable(inputText: ByteArray) : ArrayList<Pair<Byte, Int>>{
        var frequencyTable : ArrayList<Pair<Byte, Int>> = arrayListOf()
        val symbols = inputText[5].toUByte().toInt()
        println("symbols is $symbols")
        currentByte = 6
        var symbol: Byte = 0
        var freq = 0
        while (currentByte < 6 + 3 * symbols) {
            when (currentByte % 3) {
                0 -> symbol = inputText[currentByte]
                1 -> freq = freq or (inputText[currentByte].toUByte().toInt() shl 8)
                2 -> {
                    freq = freq or (inputText[currentByte].toUByte().toInt())
                    frequencyTable.add(symbol to freq)
                    freq = 0
                }
            }
            currentByte++
        }
        return frequencyTable
    }

    private fun createThirdFrequencyTable(inputText: ByteArray) : ArrayList<Pair<Byte, Int>> {
        var frequencyTable : ArrayList<Pair<Byte, Int>> = arrayListOf()
        val symbols = inputText[5].toUByte().toInt()
        currentByte = 6
        var symbol: Byte = 0
        var freq = 0
        while (currentByte < 6 + 4 * symbols) {
            when (currentByte % 4) {
                2 -> symbol = inputText[currentByte]
                3 -> freq = freq or (inputText[currentByte].toUByte().toInt() shl 16)
                0 -> freq = freq or (inputText[currentByte].toUByte().toInt() shl 8)
                1 -> {
                    freq = freq or (inputText[currentByte].toUByte().toInt())
                    frequencyTable.add(symbol to freq)
                    freq = 0
                }
            }
            currentByte++

        }
        return frequencyTable
    }

    private fun createFourthFrequencyTable(inputText: ByteArray) : ArrayList<Pair<Byte, Int>> {
        var frequencyTable : ArrayList<Pair<Byte, Int>> = arrayListOf()
        val symbols = inputText[5].toUByte().toInt()
        currentByte = 6
        var symbol: Byte = 0
        var freq = 0
        while (currentByte < 6 + 5 * symbols) {
            when (currentByte % 5) {
                1 -> symbol = inputText[currentByte]
                2 -> freq = (freq or inputText[currentByte].toUByte().toInt() shl 24)
                3 -> freq = (freq or inputText[currentByte].toUByte().toInt() shl 16)
                4 -> freq = (freq or inputText[currentByte].toUByte().toInt() shl 8)
                0 -> {
                    freq = freq or (inputText[currentByte].toUByte().toInt())
                    frequencyTable.add(symbol to freq)
                    freq = 0
                }
            }
            currentByte++
        }
        return frequencyTable
    }

    private fun decode(toDecode: ByteArray, rangeTable: Map<Byte?, Pair<Int, Int>>, length : Int) {
        //println("${toDecode[411].toUByte()} ${toDecode[412].toUByte()} ${toDecode[413].toUByte()}")
        //println()
        //for(item in toDecode){
        //    for( i in 1..8){
        //        print("${(item.toInt() shr (8-i)) and 1}")
        //    }
        //}
        //println()
        println(currentByte)

        var lowBorder = 0
        var highBorder = MAX_HEIGHT
        var soOutput = arrayListOf<Byte>()
        var value = computeStartValue(toDecode)
        var indexOfBit = 1
        var str = ""
        //println(length)
        for (i in 1 until length) {
            //println(" dec $i, $length")
            //Log.d("myLogs", " $i value is $value")
            var frequency = ((value - lowBorder + 1) * (length) - 1) / (highBorder - lowBorder + 1)
            var symbol: Byte? = rangeTable.keys.elementAt(1)
            var boolean = false //костыль, надо было range table в массив перебить, но увы
            rangeTable.forEach {
                if (it.key != null) {
                    if (it.value.second <= frequency) symbol = it.key
                    else if (!boolean) {
                        boolean = true
                        symbol = it.key
                    }
                }
            }
            //if(symbol == 'x'.toByte()) Log.e("HUY", "$i $currentByte ${toDecode[currentByte-1].toUByte()} ${toDecode[currentByte].toUByte()} ${toDecode[currentByte+1].toUByte()}}")
            val oldLowBorder = lowBorder
            lowBorder = oldLowBorder + rangeTable[symbol]!!.first * (highBorder - oldLowBorder + 1) / length
            highBorder = oldLowBorder + rangeTable[symbol]!!.second * (highBorder - oldLowBorder + 1) / length - 1
            while (true) {
                //println("$i value is $value")
                if (highBorder < HALF) {
                    //there is nothing
                } else if (lowBorder >= HALF) {
                    lowBorder -= HALF
                    highBorder -= HALF
                    value -= HALF
                } else if ((lowBorder >= QUARTER) and (highBorder < THREE_QUARTERS)) {
                    lowBorder -= QUARTER
                    highBorder -= QUARTER
                    value -= QUARTER
                } else break
                lowBorder += lowBorder
                highBorder += highBorder + 1
                if(toDecode.isNotEmpty()){
                    var b = getNewBit(toDecode, currentByte, indexOfBit)
                    //str += (b.toByte() + 48).toChar()
                    if(currentByte in 411..413) println("${currentByte} $b")
                    //if(i % 10 == 0) Log.d("HUY", " $i ${getNewBit(toDecode, currentByte, indexOfBit)}")
                    value += value + b
                    if(indexOfBit == 8){
                        indexOfBit = 1
                        currentByte++
                    } else {
                        indexOfBit++
                    }
                } else {
                    value += value
                }
            }

            soOutput.add(symbol!!)
            if (soOutput.size % 40000 == 0) {
                CoderService.updateInfo(soOutput.toByteArray(), currentByte, size)
                soOutput = arrayListOf()
            }
        }
        CoderService.updateInfo(soOutput.toByteArray(), 0, 0)
        CoderService.sendLastDecodeNotification()
    }

    private fun getNewBit(toDecode: ByteArray, index : Int, indexOfBit : Int) : Int{
        var bit = 0
        if(toDecode.indices.contains(index)){
            bit = toDecode[index].toInt()
            bit = bit shr (8-indexOfBit)
        }
        return bit and 1
    }

    private fun computeStartValue(toDecode: ByteArray): Int {
        //for(byte in toDecode){
        //    Log.d("myLogs", "${byte.toUByte()}")
        //}
        //Log.d("myLogs", "${toDecode[currentByte].toUByte() } and ${toDecode[currentByte+1].toUByte()}")
        var value = ((toDecode[currentByte].toUByte().toUInt()) shl 8) +
                toDecode[currentByte+1].toUByte().toUInt()
        currentByte+=2
        return value.toInt()
    }
}