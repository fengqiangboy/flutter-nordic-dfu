#import "FlutterNordicDfuPlugin.h"
#import <flutter_nordic_dfu/flutter_nordic_dfu-Swift.h>

@implementation FlutterNordicDfuPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterNordicDfuPlugin registerWithRegistrar:registrar];
}
@end
