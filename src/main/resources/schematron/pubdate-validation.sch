<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:hl="urn:hl"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            queryBinding="xslt2"
            schemaVersion="iso">

    <sch:title>Pubdate and year validation</sch:title>

    <sch:pattern id="pubdate-consistency">
        <sch:rule context="pubdate">
            <sch:let name="attr" value="normalize-space(@pubdate)"/>
            <sch:let name="text" value="normalize-space(.)"/>
            <sch:let name="parts" value="tokenize($text, '\s+')"/>
            <sch:let name="day" value="if (count($parts) >= 1) then format-number(number($parts[1]), '00') else ''"/>
            <sch:let name="month-name" value="if (count($parts) >= 2) then $parts[2] else ''"/>
            <sch:let name="year" value="if (count($parts) >= 3) then $parts[3] else ''"/>
            <sch:let name="month-num" value="hl:month-to-number($month-name)"/>

            <sch:assert test="$day != '' and $month-num != '' and $year != ''"
                        id="date-format-error">
                The textual pubdate "<sch:value-of select="$text"/>" must be in the format "DD Month YYYY".
            </sch:assert>

            <sch:assert test="concat($year, '-', $month-num, '-', $day) = $attr"
                        id="pubdate-attr-mismatch">
                The pubdate attribute value "<sch:value-of select="$attr"/>" must match
                the textual date "<sch:value-of select="$text"/>"
                (expected: "<sch:value-of select="concat($year, '-', $month-num, '-', $day)"/>").
            </sch:assert>

            <sch:assert test="normalize-space(../year) = substring($attr, 1, 4)"
                        id="year-mismatch">
                The year element value "<sch:value-of select="normalize-space(../year)"/>"
                must match the year portion of the pubdate attribute
                ("<sch:value-of select="substring($attr, 1, 4)"/>").
            </sch:assert>
        </sch:rule>
    </sch:pattern>

    <xsl:function xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                  name="hl:month-to-number" as="xs:string">
        <xsl:param name="name" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$name = 'January'   or $name = 'Jan'">01</xsl:when>
            <xsl:when test="$name = 'February'  or $name = 'Feb'">02</xsl:when>
            <xsl:when test="$name = 'March'     or $name = 'Mar'">03</xsl:when>
            <xsl:when test="$name = 'April'     or $name = 'Apr'">04</xsl:when>
            <xsl:when test="$name = 'May'                    ">05</xsl:when>
            <xsl:when test="$name = 'June'      or $name = 'Jun'">06</xsl:when>
            <xsl:when test="$name = 'July'      or $name = 'Jul'">07</xsl:when>
            <xsl:when test="$name = 'August'    or $name = 'Aug'">08</xsl:when>
            <xsl:when test="$name = 'September' or $name = 'Sep' or $name = 'Sept'">09</xsl:when>
            <xsl:when test="$name = 'October'   or $name = 'Oct'">10</xsl:when>
            <xsl:when test="$name = 'November'  or $name = 'Nov'">11</xsl:when>
            <xsl:when test="$name = 'December'  or $name = 'Dec'">12</xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</sch:schema>
