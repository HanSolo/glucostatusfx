#ifndef __EU_HANSOLO_FX_GLUCOSTATUS_MAIN_H
#define __EU_HANSOLO_FX_GLUCOSTATUS_MAIN_H

#include <graal_isolate_dynamic.h>


#if defined(__cplusplus)
extern "C" {
#endif

typedef int (*run_main_fn_t)(int argc, char** argv);

typedef void (*vmLocatorSymbol_fn_t)(graal_isolatethread_t* thread);

#if defined(__cplusplus)
}
#endif
#endif
