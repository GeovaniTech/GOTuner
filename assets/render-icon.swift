import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
let size = 1024
let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size*4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
ctx.scaleBy(x: CGFloat(size)/108, y: CGFloat(size)/108)
ctx.translateBy(x:0,y:108); ctx.scaleBy(x:1,y:-1)
let dark = CGColor(red:16/255,green:19/255,blue:27/255,alpha:1)
let green = CGColor(red:102/255,green:227/255,blue:170/255,alpha:1)
ctx.setFillColor(dark);ctx.fill(CGRect(x:0,y:0,width:108,height:108))
ctx.translateBy(x:11,y:-5);ctx.translateBy(x:43,y:57);ctx.rotate(by:35 * .pi/180);ctx.translateBy(x:-43,y:-57)
let p = CGMutablePath();p.move(to:CGPoint(x:44,y:54))
p.addCurve(to:CGPoint(x:32,y:65),control1:CGPoint(x:34,y:49),control2:CGPoint(x:28,y:57))
p.addCurve(to:CGPoint(x:25,y:81),control1:CGPoint(x:34,y:70),control2:CGPoint(x:25,y:71))
p.addCurve(to:CGPoint(x:43,y:94),control1:CGPoint(x:25,y:92),control2:CGPoint(x:34,y:97))
p.addCurve(to:CGPoint(x:48,y:77),control1:CGPoint(x:54,y:90),control2:CGPoint(x:52,y:81))
p.addCurve(to:CGPoint(x:52,y:62),control1:CGPoint(x:44,y:73),control2:CGPoint(x:54,y:70))
p.addCurve(to:CGPoint(x:44,y:54),control1:CGPoint(x:51,y:57),control2:CGPoint(x:48,y:55));p.closeSubpath()
ctx.setFillColor(green);ctx.addPath(p);ctx.fillPath()
ctx.fill(CGRect(x:40,y:29,width:6,height:33))
ctx.beginPath();ctx.move(to:CGPoint(x:39,y:20));ctx.addLine(to:CGPoint(x:47,y:20));ctx.addLine(to:CGPoint(x:48,y:32));ctx.addLine(to:CGPoint(x:38,y:32));ctx.closePath();ctx.fillPath()
ctx.setFillColor(dark);ctx.fillEllipse(in:CGRect(x:36,y:67,width:10,height:10))
ctx.setStrokeColor(dark);ctx.setLineWidth(1.5);ctx.setLineCap(.round)
for (a,b) in [(CGPoint(x:36,y:85),CGPoint(x:46,y:85)),(CGPoint(x:41,y:33),CGPoint(x:41,y:66)),(CGPoint(x:45,y:33),CGPoint(x:45,y:66))] {ctx.move(to:a);ctx.addLine(to:b);ctx.strokePath()}
let url = URL(fileURLWithPath:CommandLine.arguments[1])
let output = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(output,ctx.makeImage()!,nil);CGImageDestinationFinalize(output)
