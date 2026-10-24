<template>
  <div class="fixed-site-background" aria-hidden="true">
    <svg class="site-linework site-linework--upper" viewBox="0 0 720 640" fill="none" focusable="false">
      <path d="M60 -80C30 215 220 440 760 460M120 -80C90 180 260 380 760 400M180 -80C150 145 300 320 760 340M240 -80C210 110 340 260 760 280" />
    </svg>
    <svg class="site-linework site-linework--lower" viewBox="0 0 720 640" fill="none" focusable="false">
      <g transform="translate(720 640) rotate(180)"><path d="M120 -80C90 180 260 380 760 400M180 -80C150 145 300 320 760 340M240 -80C210 110 340 260 760 280" /></g>
    </svg>
    <svg class="site-note site-note--small" viewBox="0 0 48 64" fill="none" focusable="false"><path d="M30 44V10C31 18 41 19 41 28" /><ellipse cx="22" cy="47" rx="8" ry="6" transform="rotate(-18 22 47)" /></svg>
    <svg class="site-note site-note--large" viewBox="0 0 64 64" fill="none" focusable="false"><path d="M22 47V18L50 10V39M22 25L50 17" /><ellipse cx="15" cy="49" rx="7" ry="5" transform="rotate(-18 15 49)" /><ellipse cx="43" cy="41" rx="7" ry="5" transform="rotate(-18 43 41)" /></svg>
  </div>
</template>

<style scoped>
.fixed-site-background { position: fixed; inset: 118px 0 0; z-index: -1; overflow: hidden; pointer-events: none; }
.site-linework { position: absolute; width: 820px; height: auto; stroke: #fff; stroke-width: 1.25; opacity: .28; }
.site-linework--upper { top: -140px; right: -140px; }
.site-linework--lower { bottom: -140px; left: -180px; opacity: .24; }
.site-note { position: absolute; stroke: #fff; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.site-note--small { width: 30px; height: 40px; left: 6%; top: 34%; opacity: .3; transform: rotate(-14deg); }
.site-note--large { width: 52px; height: 52px; right: 7%; bottom: 14%; opacity: .23; transform: rotate(12deg); }
@media (max-width: 760px) { .fixed-site-background { top: 138px; } }
@media (max-width: 640px) {
  .site-linework { width: 560px; }
  .site-linework--upper { right: -240px; top: -120px; opacity: .22; }
  .site-linework--lower { left: -260px; bottom: -160px; opacity: .2; }
  .site-note--small { left: 8px; top: 48%; width: 22px; opacity: .18; }
  .site-note--large { right: 10px; bottom: 8%; width: 34px; opacity: .16; }
}
</style>
