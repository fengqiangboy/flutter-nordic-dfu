#import "FlutterNordicDfuPlugin.h"
#if __has_include(<flutter_nordic_dfu/flutter_nordic_dfu-Swift.h>)
#import <flutter_nordic_dfu/flutter_nordic_dfu-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_nordic_dfu-Swift.h"
#endif

@implementation FlutterNordicDfuPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterNordicDfuPlugin registerWithRegistrar:registrar];
}
@end
