import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import desktopLck from "@/assets/LCKbanner.png";
import desktopVal from "@/assets/VALbanner.png";
import desktopPubg from "@/assets/PUBGbanner.png";
import desktopWatch from "@/assets/WATCHbanner.png";
import mobileLck from "@/assets/LCKmobile.png";
import mobileVal from "@/assets/VALmobile.png";
import mobilePubg from "@/assets/PUBGmobile.png";
import mobileWatch from "@/assets/WATCHmobile.png";

const banners = [
  {image:desktopLck,
    mobileImage: mobileLck,
    link: "/events/3952",
   },
  {image:desktopVal,
    mobileImage: mobileVal,
    link: "/events/3954",
   },
  {image:desktopPubg,
    mobileImage: mobilePubg,
    link: "/events/3957",
   },
  {image:desktopWatch,
    mobileImage: mobileWatch,
    link: "/events/3955",
   },
  ];


export function HeroSection() {
  const [current, setCurrent] = useState(0);

  const prevSlide = () => {
    setCurrent((prev) =>
      prev === 0 ? banners.length - 1 : prev - 1
    );
  };

  const nextSlide = () => {
    setCurrent((prev) =>
      (prev + 1) % banners.length
    );
  };

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrent((prev) => (prev + 1) % banners.length);
    }, 10000); // 10초

    return () => clearInterval(timer);
  }, []);

  return (
    <section className="flex justify-center px-6 py-6">
      <div className="relative group mx-auto
            h-[300px] md:h-[400px]
            w-full max-w-7xl
            overflow-hidden rounded-3xl">
        <div
            className="flex h-full transition-transform duration-700 ease-in-out" 
            style={{
              transform: `translateX(-${current * 100}%)`,
              display: 'flex'
            }}
          >
          {banners.map((banner, index) => (
            <Link
              key={index}
              to={banner.link}
              className="relative h-full min-w-full block"
            >
          <div className="relative h-full min-w-full">
            <picture>
            <source
              media="(max-width: 768px)"
              srcSet={banner.mobileImage}
            />
            <img
              src={banner.image}
              alt={`banner-${index}`}
              className="w-full h-full object-cover"
            />
            </picture>
            <div className="absolute inset-0 bg-black/20 hover:bg-black/10 transition" />
            
          </div>
          </Link>
        ))}
      </div>


      {/* 하단 인디케이터 */}
      <div className="absolute bottom-6 left-1/2 z-20 flex -translate-x-1/2 gap-2">
        {banners.map((_, index) => (
          <button
            key={index}
            onClick={() => setCurrent(index)}
            className={`h-2 transition-all rounded-full ${
              current === index
                ? "w-8 bg-white"
                : "w-2 bg-white/50"
            }`}
          />
        ))}
      </div>

      <button
      onClick={prevSlide}
      className="
        absolute left-4 top-1/2 -translate-y-1/2 z-20
        h-12 w-12 rounded-full
        bg-black/30 backdrop-blur-sm
        text-white
        opacity-0
        group-hover:opacity-100
        transition-all duration-300
      "
    >
      ‹
    </button>

    <button
      onClick={nextSlide}
      className="
        absolute right-4 top-1/2 -translate-y-1/2 z-20
        h-12 w-12 rounded-full
        bg-black/30 backdrop-blur-sm
        text-white
        opacity-0
        group-hover:opacity-100
        transition-all duration-300
      "
    >
      ›
    </button>
    </div>
    </section>
  );
}