import Foundation
import ScreenCaptureKit
import AVFoundation
import AppKit

final class Recorder: NSObject, SCRecordingOutputDelegate, SCStreamDelegate {
    var finished = false
    func recordingOutputDidStartRecording(_ recordingOutput: SCRecordingOutput) { print("RECORDING_STARTED \(Date().timeIntervalSince1970)"); fflush(stdout) }
    func recordingOutputDidFinishRecording(_ recordingOutput: SCRecordingOutput) { finished = true; print("RECORDING_FINISHED") }
    func recordingOutput(_ recordingOutput: SCRecordingOutput, didFailWithError error: any Error) { print("RECORDING_ERROR \(error)"); finished = true }
    func stream(_ stream: SCStream, didStopWithError error: any Error) { print("STREAM_STOP \(error)") }
}
@main struct Main {
 static func main() async throws {
    _ = NSApplication.shared
    let args=CommandLine.arguments
    let path=args[1], seconds=Double(args[2]) ?? 180
    var window: SCWindow? = nil
    for _ in 0..<240 {
        let content=try await SCShareableContent.excludingDesktopWindows(true,onScreenWindowsOnly:false)
        window=content.windows.first { $0.frame.width >= 1280 && ($0.title ?? "").contains("Minecraft") && ($0.owningApplication?.applicationName ?? "").lowercased().contains("java") }
        if window != nil {break}
        try await Task.sleep(nanoseconds:500_000_000)
    }
    guard let window else {throw NSError(domain:"Game window not found",code:1)}
    print("CAPTURE_WINDOW \(window.windowID) \(window.title ?? "") \(window.frame)")
    let filter=SCContentFilter(desktopIndependentWindow:window)
    let config=SCStreamConfiguration()
    config.width=1280;config.height=720;config.minimumFrameInterval=CMTime(value:1,timescale:30)
    config.capturesAudio=true;config.captureMicrophone=false;config.sampleRate=48000;config.channelCount=2
    config.showsCursor=false;config.ignoreShadowsSingleWindow=true
    config.sourceRect=CGRect(x:0,y:28,width:1280,height:720)
    let recorder=Recorder()
    let stream=SCStream(filter:filter,configuration:config,delegate:recorder)
    let rc=SCRecordingOutputConfiguration();rc.outputURL=URL(fileURLWithPath:path);rc.videoCodecType = .h264;rc.outputFileType = .mp4
    let output=SCRecordingOutput(configuration:rc,delegate:recorder)
    try stream.addRecordingOutput(output)
    try await stream.startCapture()
    try String(Date().timeIntervalSince1970).write(toFile:"/tmp/doomcraft-recording-ready",atomically:true,encoding:.utf8)
    for _ in 0..<Int(seconds*2) { if recorder.finished {break};try await Task.sleep(nanoseconds:500_000_000) }
    do {try await stream.stopCapture()} catch {
      if (error as NSError).code != -3808 {throw error}
    }
    for _ in 0..<100 {if recorder.finished {break};try await Task.sleep(nanoseconds:100_000_000)}
 }
}
