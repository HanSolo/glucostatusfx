#ifndef __EU_HANSOLO_FX_GLUCOSTATUS_MAIN_H
#define __EU_HANSOLO_FX_GLUCOSTATUS_MAIN_H

#include <graal_isolate.h>


#if defined(__cplusplus)
extern "C" {
#endif

int run_main(int argc, char** argv);

void vmLocatorSymbol(graal_isolatethread_t* thread);

#if defined(__cplusplus)
}
#endif
#endif
