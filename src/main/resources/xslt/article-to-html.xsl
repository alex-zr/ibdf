<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs">

    <xsl:param name="collectionName" as="xs:string" select="''"/>

    <xsl:output method="html" version="5.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <html>
            <head>
                <meta charset="UTF-8"/>
                <title>
                    <xsl:value-of select="normalize-space(/country-chap/chaphead/title)"/>
                    <xsl:text> - </xsl:text>
                    <xsl:value-of select="$collectionName"/>
                </title>
            </head>
            <body>
                <header>
                    <h1 class="article-title">
                        <xsl:apply-templates select="/country-chap/chaphead/title/node()"/>
                    </h1>
                </header>
                <main>
                    <xsl:apply-templates select="/country-chap/chapbody/section"/>
                </main>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="section">
        <xsl:variable name="depth" select="count(ancestor::section)"/>
        <xsl:variable name="level" select="if ($depth + 1 > 6) then 6 else $depth + 1"/>
        <xsl:element name="h{$level}">
            <xsl:attribute name="class">section-heading</xsl:attribute>
            <span class="label">
                <xsl:value-of select="@label"/>
                <xsl:text> </xsl:text>
            </span>
            <span class="title">
                <xsl:apply-templates select="title/node()"/>
            </span>
        </xsl:element>
        <xsl:apply-templates select="section"/>
    </xsl:template>

    <xsl:template match="sub">
        <sub>
            <xsl:apply-templates/>
        </sub>
    </xsl:template>

    <xsl:template match="emph[@type='i']">
        <i>
            <xsl:apply-templates/>
        </i>
    </xsl:template>

    <xsl:template match="footnote"/>

</xsl:stylesheet>
