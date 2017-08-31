# -*- coding: utf-8 -*-
from reportlab.pdfgen.canvas import Canvas
from reportlab.lib.units import *
from reportlab.lib.pagesizes import *
from reportlab.graphics.barcode.common import *
from reportlab.platypus.paragraph import Paragraph
from reportlab.platypus.flowables import Flowable
from datetime import *
from xml.dom.minidom import *
from StringIO import *
import sys
import getopt
import os

def formatThousands(value):
    if len(value) > 3:
        return formatThousands(value[:-3]) + "," + value[-3:]
    else:
        return value

def formatMonetary(value):
    return formatThousands(value[:-2].strip()) + "." + value[-2:]
    
def unformat(value):
    retval = StringIO()
    for char in value:
        if char.isdigit():
            retval.write(char)
    return int(retval.getvalue())
    
def reverse_str(s):
    new_s = []
    for char in s:
        new_s.append(char)        
    new_s.reverse()
    return new_s

def modulo10(numero):
    sum = 0
    fator = 2
    for char in reverse_str(numero):
        value = int(char) * fator
        if value < 10:
           sum += value
        else:
           sum += (value % 10) + 1
        fator = 2 / fator
    dac = 10 - sum % 10
    return "0" if dac == 10 else str(dac)

def modulo11(numero):
    sum = 0
    fator = 2
    for char in reverse_str(numero):
        sum += int(char) * fator
        fator = fator + 1 if fator < 9 else 2
    dac = 11 - sum % 11
    return "1" if dac == 0 or dac > 9 else str(dac)

class CodBarras():
    def __init__(self, data):
        self.values = data

    def fator(self, vencimento):
        if vencimento.day < 10:
            data = date(vencimento.year, vencimento.month, 10)
        else:
            if vencimento.month < 12:
               data = date(vencimento.year, vencimento.month + 1, 10)
            else:
               data = date(vencimento.year + 1, 1, 10)
        return "%04d" % ((data - date(2000, 7, 3)).days + 1000)

    def beautify_group(self, group):
        return group[0:5] + "." + group[5:]

    def numeric(self):
        groups = [self.values["banco"][0:3] + "9" + self.values["carteira"] + self.values["nossonumero"][0:2],\
                  self.values["nossonumero"][2:8] + modulo10(self.values["agencia"] + self.values["conta"][0:5] + self.values["carteira"] + self.values["nossonumero"]) + \
                                             self.values["agencia"][0:3], \
                  self.values["agencia"][3] + self.values["conta"] + "000",
                  modulo11(self.rawBarcode()),
                  self.fator(self.values["vencimento"]) + ("%010d" % unformat(self.values["valor"]))]
        groups[0] += modulo10(groups[0])
        groups[1] += modulo10(groups[1])
        groups[2] += modulo10(groups[2])
        return self.beautify_group(groups[0]) + "  " + \
               self.beautify_group(groups[1]) + "   " + \
               self.beautify_group(groups[2]) + "  " + \
               groups[3] + "  " + groups[4]
        
    def rawBarcode(self):
        return self.values["banco"][0:3] + "9" + self.fator(self.values["vencimento"]) + ("%010d" % unformat(self.values["valor"])) +\
               self.values["carteira"] + self.values["nossonumero"] + modulo10(self.values["agencia"] + self.values["conta"][0:5] + self.values["carteira"] + \
               self.values["nossonumero"]) + self.values["agencia"] + self.values["conta"] + "000"

    def barCode(self):
        numero = self.rawBarcode()
        return numero[0:4] + modulo11(numero) + numero[4:]
        
class Pedaco(Flowable):
    def __init__(self, values):
        self.width = A4[0] - 2 * cm
        self.values = values
        
    def drawLines(self, lines = []):
        self.canv.drawImage(os.path.join(os.path.abspath("."),"itau.jpg"), 0.2 * cm, -1.3 * cm, 1 * cm, 1 * cm)
        self.canv.setLineWidth(0.05 * cm)
        self.canv.line(0, -1.4 * cm, self.width, -1.4 * cm)
        self.canv.line(4.6 * cm, -0.7 * cm, 4.6 * cm, -1.4 * cm)
        self.canv.line(6.6 * cm, -0.7 * cm, 6.6 * cm, -1.4 * cm)
        self.canv.setLineWidth(0.01 * cm)
        for i in lines:
            self.canv.line(0, -i * cm, self.width, -i * cm)
        self.canv.line(15.2 * cm, -1.4 * cm, 15.2 * cm, -7.4 * cm)
        for i in (5, 5.6, 6.2, 6.8, 7.4):
            self.canv.line(15.2 * cm, -i * cm, self.width, -i * cm)
        for i in (4.5, 6.8, 9.4, 11.4):
            self.canv.line(i * cm, -3.1 * cm, i * cm, -3.7 * cm)
        for i in (3.4, 6.4, 8.1, 11.4):
            self.canv.line(i * cm, -3.7 * cm, i * cm, -4.3 * cm)

    def drawLabels(self, extra = []):
        self.canv.setFont("Helvetica", 6)
        labels = [(0.15, 1.65, "Local de Pagamento"), (15.35, 1.65, "Vencimento"),\
                  (0.15, 2.7, "Cedente"), (15.35, 2.7, "AgÃªncia/CÃ³digo Cedente"),\
                  (0.15, 3.3, "Data do Documento"), (4.65, 3.3, "No. do Documento"),\
                  (6.95, 3.3, "EspÃ©cie Doc."), (9.55, 3.3, "Aceite"),\
                  (11.55, 3.3, "Data do processamento"), (15.35, 3.3, "Nosso NÃºmero"),\
                  (0.15, 3.9, "Uso do banco"), (3.55, 3.9, "Carteira"),\
                  (6.55, 3.9, "EspÃ©cie"), (8.25, 3.9, "Quantidade"),\
                  (11.55, 3.9, "Valor"), (15.35, 3.9, "(=) Valor do Documento"),\
                  (15.35, 4.5, "(-) Desconto / Abatimento"), (15.35, 5.8, "(+) Mora/Multa")]
        labels.extend(extra)
        for label in labels:
            self.canv.drawString(label[0] * cm, -label[1] * cm, label[2])
        self.canv.setFont("Helvetica-Bold", 10)
        self.canv.drawString(1.5 * cm, -1.0 * cm, "Banco ItaÃº SA")        
        self.canv.setFont("Helvetica", 8)
        self.canv.drawString(10.1 * cm, -3.6 * cm, "N")        
        self.canv.setFont("Helvetica-Bold", 16)
        self.canv.drawString(4.9 * cm, -1.2 * cm, "%s-%s" % (self.values["banco"][0:3], self.values["banco"][3]))

    def drawValues(self, extra = []):
        left = [(0.15, 2.0, "aviso"), (0.15, 3.0, "cedente"), \
                (4.7, 3.6, "numerodoc"), (7.7, 3.6, "tipodoc"),\
                (3.8, 4.2, "carteira"), (6.6, 4.2, "especie")]
        right = [(4.2, "valor")]
        left.extend(extra)        
        rightMargin = self.width - 0.6 * cm
        # Vencimento
        self.canv.setFont("Helvetica", 10)
        self.canv.drawRightString(self.width - 0.6 * cm, -2.0 * cm, self.values["vencimento"].strftime("%d/%m/%Y"))
        self.canv.setFont("Helvetica", 8)
        self.canv.drawString(0.15 * cm, -3.6 * cm, self.values["emissao"].strftime("%d/%m/%Y"))
        self.canv.drawRightString(rightMargin, -3.0 * cm, "%s/%s-%s" % (self.values["agencia"], self.values["conta"][0:5], self.values["conta"][5]))
        self.canv.drawRightString(rightMargin, -3.6 * cm, "%s-%s" % (self.values["nossonumero"], \
                                  modulo10(self.values["agencia"] + self.values["conta"][0:5] + self.values["carteira"] + \
                                           self.values["nossonumero"])))
        self.canv.drawRightString(rightMargin, -4.2 * cm, self.values["valor"])
        # Valores alinhados Ã  esquerda
        for item in left:
            if item[2] in self.values:
                line_count = 0
                for line in self.values[item[2]].splitlines():
                    self.canv.drawString(item[0] * cm, -item[1] * cm - 8 * line_count, line)
                    line_count += 1
        # NÃºmero do cÃ³digo de barras
        self.canv.setFont("Helvetica", 11.5)
        codbarras = CodBarras(self.values)
        self.canv.drawString(7.0 * cm, -1.2 * cm, codbarras.numeric())

    def draw(self):
        self.drawLines()
        self.drawLabels()
        self.drawValues()

class Recibo(Pedaco):
            
    def drawLines(self):
        Pedaco.drawLines(self, [2.5, 3.1, 3.7, 4.3, 10.0, 11.5])
        
    def drawLabels(self):
        Pedaco.drawLabels(self, [(0.15, 4.5, "InformaÃ§Ãµes"),(15.35, 7.0, "(=) Valor Cobrado"), (0.15, 10.2, "Sacado")])
        self.canv.setFont("Helvetica-Bold", 7)
        self.canv.drawString(11.2 * cm, -11.8 * cm, "AutenticaÃ§Ã£o MecÃ¢nica")        

    def drawValues(self):
        Pedaco.drawValues(self, [(0.15, 4.9, "detalhe"), (0.15, 10.5, "sacado")]) 

class Boleto(Pedaco):
    def drawLines(self):
        Pedaco.drawLines(self, [2.5, 3.1, 3.7, 4.3, 7.4, 8.9])
        
    def drawLabels(self):
        Pedaco.drawLabels(self, [(0.15, 4.5, "InstruÃ§Ãµes (Todas informaÃ§Ãµes desde bloqueto sÃ£o de exclusiva responsabilidade do cedente)"),\
                                 (15.35, 7.0, "(=) Valor Cobrado"), (0.15, 7.6, "Sacado"),\
                                 (0.15, 8.8, "Sacador Avalista"), (11.5, 8.8, "CÃ³digo de Baixa:")])
        self.canv.setFont("Helvetica-Bold", 7)
        self.canv.drawString(11.2 * cm, -9.2 * cm, "AutenticaÃ§Ã£o MecÃ¢nica/FICHA DE COMPENSAÃ‡ÃƒO")        
        
    def drawValues(self):
        Pedaco.drawValues(self, [(0.15, 4.9, "instrucoes"), (0.15, 7.9, "sacado")])
        p = I2of5(CodBarras(self.values).barCode(), checksum = 0, bearers = 0, barWidth = 0.7, gap = 2)
        p.drawOn(self.canv, 0, -10.4 * cm)

def Pagina(canvas, data):
    canvas.setDash(4,4)
    canvas.setLineWidth(0.01 * cm)
    canvas.line(1 * cm, 12.2 * cm, A4[0] - 1 * cm, 12.2 * cm)
    canvas.setDash()
    Recibo(data).drawOn(canvas, 1 * cm, A4[1] - 1.5 * cm)
    Boleto(data).drawOn(canvas, 1 * cm, 12 * cm)

def parseXml(filename):
    doc = xml.dom.minidom.parse(filename)
    boletos = []
    defaultFormatter = lambda(value) : "\n".join(value.split('\\n'))
    formatters = {'aviso': defaultFormatter,\
                  'cedente': defaultFormatter,\
                  'detalhe': defaultFormatter,\
                  'instrucoes': defaultFormatter,\
                  'sacado': defaultFormatter,\
                  'valor': formatMonetary,\
                  'vencimento': lambda(value) : date(int(value[0:4]), int(value[4:6]), int(value[6:8]))}

    for boleto in [item for item in doc.documentElement.childNodes if item.nodeType == Element.ELEMENT_NODE]:
        hash = {}
        for field in [item for item in boleto.childNodes if item.nodeType == Element.ELEMENT_NODE and len(item.childNodes)]:
            if field.nodeName in formatters:
                hash[field.nodeName] = formatters[field.nodeName](field.childNodes[0].nodeValue)
            else:
                hash[field.nodeName] = field.childNodes[0].nodeValue
        boletos.append(hash)
    return boletos

def main(argv=None):
    if argv is None:
        argv = sys.argv
    opts, args = getopt.getopt(argv[1:], None)
    boletos = parseXml(args[0])
    fixed =  {"carteira" : "109", "agencia" : "1338", "conta" : "451808", "banco" : "3417", \
              "especie" : "R$",  "tipodoc" : "RC", "emissao": date.today() }
    canvas = Canvas("boleto.pdf", A4)
    for boleto in boletos:
        Pagina(canvas, dict(boleto, **fixed))
        canvas.showPage()
    canvas.save()
        
if __name__ == "__main__":
    sys.exit(main())